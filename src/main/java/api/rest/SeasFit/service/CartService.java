package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.AddToCartDTO;
import api.rest.SeasFit.dto.CartItemDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final ProductVariantRepository productVariantRepository;

    /* ======================= Helpers ======================= */

    private BigDecimal nvl(BigDecimal x) { return x != null ? x : BigDecimal.ZERO; }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(null, user, LocalDateTime.now())));
    }

    /* ======================= Queries/Mutations ======================= */

    public ProductVariant getProductVariantByItemId(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return item.getVariant();
    }

    @Transactional
    public void deleteItems(List<Long> ids, Long userId) {
        cartItemRepository.deleteByCart_User_IdAndIdIn(userId, ids);
    }

    @Transactional
    public ResponseEntity<?> addToCart(Long userId, AddToCartDTO req) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Cart cart = getOrCreateCart(user);

            // validate entity tồn tại
            productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            colorRepository.findById(req.getColorId())
                    .orElseThrow(() -> new RuntimeException("Color not found"));
            sizeRepository.findById(req.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Size not found"));

            ProductVariant variant = productVariantRepository
                    .findByProductIdAndColorIdAndSizeId(req.getProductId(), req.getColorId(), req.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));

            if (!variant.isActive()) {
                return ResponseEntity.badRequest().body("Biến thể hiện không mở bán");
            }

            Optional<CartItem> existing = cartItemRepository.findByCartAndVariant(cart, variant);
            int newQty = existing.map(ci -> ci.getQuantity() + req.getQuantity()).orElse(req.getQuantity());

            if (variant.getQuantity() != null && variant.getQuantity() < newQty) {
                return ResponseEntity.badRequest().body("Không đủ tồn kho");
            }

            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(newQty);
                cartItemRepository.save(item);
                return ResponseEntity.ok("Đã cập nhật số lượng sản phẩm trong giỏ hàng.");
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setVariant(variant);
                newItem.setQuantity(req.getQuantity());
                cartItemRepository.save(newItem);
                return ResponseEntity.ok("Đã thêm sản phẩm vào giỏ hàng.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FE cần finalPrice/onSale => map DTO có tính sale theo saleAmount/saleFrom/saleTo của Variant */
    public List<CartItemDTO> getCartItems(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = getOrCreateCart(user);

        // nếu bị N+1, đổi sang query join-fetch trong repository
        return cartItemRepository.findByCart(cart).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ResponseEntity<?> removeItem(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Item does not belong to your cart");
        }

        cartItemRepository.delete(item);
        return ResponseEntity.ok("Item removed");
    }

    @Transactional
    public ResponseEntity<?> updateItemQuantity(Long userId, Long itemId, int quantity) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be greater than 0");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Item does not belong to your cart");
        }

        ProductVariant v = item.getVariant();
        if (v.getQuantity() != null && v.getQuantity() < quantity) {
            return ResponseEntity.badRequest().body("Không đủ tồn kho");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return ResponseEntity.ok("Quantity updated");
    }

    @Transactional
    public void updateItemVariant(Long userId, Long itemId, Long colorId, Long sizeId) {
        CartItem item = cartItemRepository.findByIdAndCart_User_Id(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        ProductVariant newVariant = productVariantRepository
                .findByProductIdAndColorIdAndSizeId(
                        item.getVariant().getProduct().getId(), colorId, sizeId
                )
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể phù hợp"));

        if (!newVariant.isActive()) {
            throw new RuntimeException("Biến thể hiện không mở bán");
        }

        // Nếu đã có item khác cùng variant mới -> gộp số lượng
        Optional<CartItem> dup = cartItemRepository.findByCartAndVariant(item.getCart(), newVariant);
        if (dup.isPresent() && !dup.get().getId().equals(item.getId())) {
            CartItem other = dup.get();
            int mergedQty = other.getQuantity() + item.getQuantity();
            if (newVariant.getQuantity() != null && newVariant.getQuantity() < mergedQty) {
                throw new RuntimeException("Không đủ tồn kho");
            }
            other.setQuantity(mergedQty);
            cartItemRepository.delete(item);
            cartItemRepository.save(other);
        } else {
            if (newVariant.getQuantity() != null && newVariant.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Không đủ tồn kho");
            }
            item.setVariant(newVariant);
            cartItemRepository.save(item);
        }
    }

    /* ======================= Mapping ======================= */

    private CartItemDTO toDTO(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        Color color = variant.getColor();
        Size size = variant.getSize();

        BigDecimal base = nvl(variant.getPrice());
        BigDecimal eff  = nvl(variant.getEffectivePrice());  // ✅ dùng helper của entity
        boolean onSale  = variant.isSaleActiveNow() && eff.compareTo(base) < 0;

        // Ảnh: tuỳ schema; đang lấy ảnh product
        String imageUrl = product.getImageUrl();

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(imageUrl)

                .price(base)          // giá gốc
                .finalPrice(eff)      // ✅ giá sau sale (saleAmount đã clamp >= 0)
                .onSale(onSale)

                .quantity(item.getQuantity())
                .colorId(color.getId())
                .colorName(color.getName())
                .colorHex(color.getHexCode())
                .sizeId(size.getId())
                .sizeLabel(size.getLabel())
                .build();
    }

    public int getCartItemCount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        return cartItemRepository.findByCart(cart)
                .stream().mapToInt(CartItem::getQuantity).sum();
    }
}
