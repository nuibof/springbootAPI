package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.OrderRequest;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders") // ✅ Đảm bảo đúng path
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final OrderRepository orderRepository;

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

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody OrderRequest request) {
        User user = getAuthenticatedUser(authHeader);
        Order order = orderService.createOrder(user.getId(), request);

        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "totalAmount", order.getTotalAmount()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getAuthenticatedUser(authHeader); // xác thực người dùng

            Optional<Order> orderOpt = orderRepository.findByIdAndUserId(id, user.getId());
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy đơn hàng"));
            }

            return ResponseEntity.ok(orderOpt.get());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }



}
