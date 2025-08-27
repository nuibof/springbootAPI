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
    private final ProductImageRepository productImageRepository;

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

    public List<CartItemDTO> getCartItems(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var cart = getOrCreateCart(user);

        return cartItemRepository.findByCartFetchAll(cart)
                .stream().map(this::toDTO).toList();
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
    public int getCartItemCount(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        return cartItemRepository.findByCart(cart)
                .stream().mapToInt(CartItem::getQuantity).sum();
    }
    private String resolveVariantImage(ProductVariant v) {
        // 1) ảnh theo màu trong product_image (product_id + color_id)
        return productImageRepository
                .findFirstByProduct_IdAndColor_IdOrderByCreatedAtAsc(
                        v.getProduct().getId(), Long.valueOf(v.getColor().getId())
                )
                .map(ProductImage::getImageUrl)
                // 2) fallback: color.imageUrl
                .orElseGet(() -> {
                    String cImg = v.getColor() != null ? v.getColor().getImageUrl() : null;
                    return (cImg != null && !cImg.isBlank())
                            ? cImg
                            // 3) fallback: product.imageUrl
                            : (v.getProduct() != null ? v.getProduct().getImageUrl() : null);
                });
    }

    private CartItemDTO toDTO(CartItem ci) {
        var v = ci.getVariant();
        var p = v.getProduct();
        var c = v.getColor();
        var s = v.getSize();

        // Giá gốc & final
        BigDecimal price = v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO;
        BigDecimal saleAmount = v.getSaleAmount() != null ? v.getSaleAmount() : BigDecimal.ZERO;
        boolean hasSaleWindow = (v.getSaleFrom() != null && v.getSaleTo() != null);
        boolean inSaleWindow = !hasSaleWindow
                || (java.time.LocalDateTime.now().isAfter(v.getSaleFrom())
                && java.time.LocalDateTime.now().isBefore(v.getSaleTo()));
        boolean onSale = v.isActive() && saleAmount.signum() > 0 && inSaleWindow;

        BigDecimal finalPrice = onSale ? price.subtract(saleAmount) : price;

        return CartItemDTO.builder()
                .id(ci.getId())
                .productId(p.getId())
                .productName(p.getName())
                .imageUrl(resolveVariantImage(v))

                .price(price)
                .finalPrice(finalPrice.max(BigDecimal.ZERO))
                .onSale(onSale)
                .saleAmount(onSale ? saleAmount : BigDecimal.ZERO)

                .quantity(ci.getQuantity())

                .colorId(c != null ? c.getId().intValue() : null)   // NOTE: bạn đang dùng Integer
                .colorName(c != null ? c.getName() : null)
                .colorHex(c != null ? c.getHexCode() : null)

                .sizeId(s != null ? s.getId().intValue() : null)    // NOTE: bạn đang dùng Integer
                .sizeLabel(s != null ? s.getLabel() : null)
                .build();
    }
}
