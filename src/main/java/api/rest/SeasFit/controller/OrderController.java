package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.OrderRequest;
import api.rest.SeasFit.dto.VoucherApplyRequest;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.repository.VoucherRepository;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.UserService;
import api.rest.SeasFit.service.VoucherService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final VoucherRepository voucherRepository;

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

    @GetMapping()
    public ResponseEntity<?> getAllOrders(@RequestHeader ("Authorization") String authHeader) {

        try {
            User user = getAuthenticatedUser(authHeader); // xác thực người dùng
            return ResponseEntity.ok(orderService.getAllOrdersByUserId(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    // Huỷ đơn hàng (nhận JSON body nhưng không dùng DTO)
    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long id,
                                         @RequestHeader("Authorization") String authHeader,
                                         @RequestBody(required = false) Map<String, Object> body) {

        User user = getAuthenticatedUser(authHeader);

        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể huỷ đơn hàng đã được xử lý");
        }

        String reason = (body != null && body.get("reason") != null) ? String.valueOf(body.get("reason")).trim() : "";
        String note   = (body != null && body.get("note")   != null) ? String.valueOf(body.get("note")).trim()   : "";

        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu lý do huỷ (reason)");
        }

        order.setStatus("CANCELLED");
        order.setCancelReason(reason.toUpperCase()); // cột mới: cancel_reason
        if (!note.isEmpty()) {
            order.setNote(note); // dùng cột note sẵn có
        }

        orderRepository.save(order); // updated_at auto

        return ResponseEntity.ok(Map.of(
                "message", "Đơn hàng đã được huỷ thành công",
                "orderId", order.getId(),
                "reason", order.getCancelReason()
        ));
    }


    @PostMapping("/voucher/apply")
    public ResponseEntity<?> applyVoucher(@RequestBody VoucherApplyRequest request){
        Voucher voucher = voucherRepository.findByCode(request.getCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));


        LocalDate today = LocalDate.now();
        if (voucher.getStartDate().isAfter(today) || voucher.getEndDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn hoặc chưa bắt đầu");
        }

        if (!voucher.getIsActive() || voucher.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không khả dụng");
        }

        if (request.getTotalAmount().compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng không đủ điều kiện áp dụng mã giảm giá");
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucher.getDiscountType().equals("fixed")) {
            discountAmount = voucher.getDiscountValue();
        } else if (voucher.getDiscountType().equals("percent")) {
            discountAmount = request.getTotalAmount()
                    .multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));
        }

        return ResponseEntity.ok(Map.of("discountAmount", discountAmount));
    }

}
