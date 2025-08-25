package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.OrderAddress;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.OrderAddressRepository;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/order-address")
@RequiredArgsConstructor
public class OrderAddressController {

    private final OrderAddressRepository orderAddressRepository;
    private final OrderRepository orderRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    private User getAuthenticatedUser(String authHeader) {
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

    private boolean isAdmin(User user) {
        String role = user.getRole();
        return role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("STAFF"));
    }

    /**
     * Lấy snapshot địa chỉ của đơn (order_address) theo orderId.
     * - Khách: chỉ xem được đơn của chính mình
     * - Admin/Staff: xem được tất cả
     */
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<?> getByOrderId(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = getAuthenticatedUser(authHeader);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // Nếu không phải admin thì phải là chủ đơn
        if (! user.getRole().equals("ROLE_ADMIN") && (order.getUserId() == null || !order.getUserId().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem địa chỉ của đơn này");
        }

        OrderAddress oa = orderAddressRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy snapshot địa chỉ"));

        return ResponseEntity.ok(OrderAddressDTO.from(oa));
    }

    @Data
    private static class OrderAddressDTO {
        private Long orderId;
        private String fullName;
        private String phone;
        private String street;
        private String ward;
        private String district;
        private String city;
        private String country;

        static OrderAddressDTO from(OrderAddress oa) {
            OrderAddressDTO dto = new OrderAddressDTO();
            dto.setOrderId(oa.getOrder().getId());
            dto.setFullName(oa.getFullName());
            dto.setPhone(oa.getPhone());
            dto.setStreet(oa.getStreet());
            dto.setWard(oa.getWard());
            dto.setDistrict(oa.getDistrict());
            dto.setCity(oa.getCity());
            dto.setCountry(oa.getCountry());
            return dto;
        }
    }
}
