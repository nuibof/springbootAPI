package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.AddToCartDTO;
import api.rest.SeasFit.dto.CartItemDTO;
import api.rest.SeasFit.entity.CartItem;
import api.rest.SeasFit.entity.ProductVariant;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.CartItemRepository;
import api.rest.SeasFit.repository.ProductVariantRepository;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.CartService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;

    private ResponseEntity<?> unauthorized(String msg) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

    private ResponseEntity<?> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
    }

    private User getAuthenticatedUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thiếu token xác thực");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        User user = userService.findByUserName(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại");
        }

        return user;
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartItemCount(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = getAuthenticatedUser(authHeader);
            int itemCount = cartService.getCartItemCount(user.getId());
            return ResponseEntity.ok(itemCount);
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AddToCartDTO dto
    ) {



        try {
            System.out.println(authHeader);
            if(authHeader.equals("Bearer null")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chưa đăng nhập!"));
            }
            User user = getAuthenticatedUser(authHeader);
            System.out.println("AddToCartDTO: " + dto);

            Optional<ProductVariant> variantOpt = productVariantRepository
                    .findByProductIdAndColorIdAndSizeId(dto.getProductId(), dto.getColorId(), dto.getSizeId());

            if (variantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy biến thể sản phẩm"));
            }

            ProductVariant variant = variantOpt.get();

            if (variant.getQuantity() < dto.getQuantity()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Số lượng tồn kho không đủ"));
            }

            Optional<CartItem> existingItemOpt = cartItemRepository
                    .findByCart_User_IdAndVariant_Id(user.getId(), variant.getId());

            int existingQuantity = existingItemOpt.map(CartItem::getQuantity).orElse(0);
            int totalRequested = existingQuantity + dto.getQuantity();

            if (totalRequested > variant.getQuantity()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vượt quá số lượng tồn kho"));
            }

            cartService.addToCart(user.getId(), dto);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật giỏ hàng"));

        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<?> removeItem(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader);
            return cartService.removeItem(user.getId(), id);
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @PostMapping("/items/delete")
    public ResponseEntity<?> deleteSelectedItems(@RequestBody List<Long> ids,
                                                 @RequestHeader("Authorization") String authHeader) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Danh sách sản phẩm cần xoá không được rỗng"));
        }

        User user = getAuthenticatedUser(authHeader);
        cartService.deleteItems(ids, user.getId());
        System.out.println("User ID: " + user.getId() + ", Deleting items: " + ids);
        return ResponseEntity.ok(Map.of("message", "Đã xoá các sản phẩm được chọn"));
    }






    @PutMapping("/item/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestParam(required = false) Long colorId,
            @RequestParam(required = false) Long sizeId,
            @RequestParam(required = false) Integer quantity,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader);

            if (quantity != null) {
                ProductVariant variant = cartService.getProductVariantByItemId(id);
                if (variant.getQuantity() < quantity) {
                    return ResponseEntity.badRequest().body("Requested quantity exceeds available stock");
                }
                cartService.updateItemQuantity(user.getId(), id, quantity);
            }

            if (colorId != null && sizeId != null) {
                cartService.updateItemVariant(user.getId(), id, colorId, sizeId);
            }

            return ResponseEntity.ok("Cập nhật giỏ hàng thành công");
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = getAuthenticatedUser(authHeader);
            List<CartItemDTO> items = cartService.getCartItems(user.getId());
            return ResponseEntity.ok(items);
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }
}
