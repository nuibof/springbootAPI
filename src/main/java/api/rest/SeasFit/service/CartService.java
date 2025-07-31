package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.AddToCartDTO;
import api.rest.SeasFit.dto.CartItemDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

    public ProductVariant getProductVariantByItemId(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return item.getVariant();
    }

    public ResponseEntity<?> addToCart(Long userId, AddToCartDTO req) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Cart cart = cartRepository.findByUser(user)
                    .orElseGet(() -> cartRepository.save(new Cart(null, user, LocalDateTime.now())));

            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            Color color = colorRepository.findById(req.getColorId())
                    .orElseThrow(() -> new RuntimeException("Color not found"));

            Size size = sizeRepository.findById(req.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Size not found"));

            ProductVariant variant = productVariantRepository
                    .findByProduct_IdAndColor_IdAndSize_Id(
                            req.getProductId(),
                            Math.toIntExact(req.getColorId()),
                            Math.toIntExact(req.getSizeId())
                    )
                    .orElseThrow(() -> new RuntimeException("Variant not found"));




            Optional<CartItem> existingItem = cartItemRepository.findByCartAndVariant(cart, variant);

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + req.getQuantity());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(null, user, LocalDateTime.now())));
        return cartItemRepository.findByCart(cart).stream()
                .map(this::toDTO)
                .toList();
    }

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

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return ResponseEntity.ok("Quantity updated");
    }

    private CartItemDTO toDTO(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        Color color = variant.getColor();
        Size size = variant.getSize();

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .price(variant.getPrice())
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

        List<CartItem> items = cartItemRepository.findByCart(cart);
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void updateItemVariant(Long userId, Long itemId, Long colorId, Long sizeId) {

        CartItem item = cartItemRepository.findByIdAndCart_User_Id(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        Product product = item.getVariant().getProduct();

        ProductVariant newVariant = productVariantRepository
                .findByProductIdAndColorIdAndSizeId(
                        item.getVariant().getProduct().getId(), colorId, sizeId
                )
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể phù hợp"));

        item.setVariant(newVariant);

        cartItemRepository.save(item);
    }


}
