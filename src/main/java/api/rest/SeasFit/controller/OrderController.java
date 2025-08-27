package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.OrderRequest;
import api.rest.SeasFit.dto.UpdateOrderRequest;
import api.rest.SeasFit.dto.VoucherApplyRequest;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.repository.VoucherRepository;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.OrderMailService;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final OrderMailService  orderMailService;

    // ===== Helpers tiền tệ =====
    private BigDecimal nvl(BigDecimal x) { return x != null ? x : BigDecimal.ZERO; }
    private BigDecimal asMoney(Number n) { return n == null ? BigDecimal.ZERO : BigDecimal.valueOf(n.longValue()); }

    /** Quy ước: totalAmount = merchandise subtotal (sau sale, trước voucher & ship) */
    private BigDecimal payableOf(Order o) {
        BigDecimal total = asMoney(o.getTotalAmount());
        BigDecimal ship  = asMoney(o.getShippingFee());
        BigDecimal disc  = nvl(o.getDiscountAmount());
        BigDecimal res   = total.add(ship).subtract(disc);
        return res.signum() < 0 ? BigDecimal.ZERO : res.setScale(0, RoundingMode.HALF_UP);
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

    private Long getAuthenticatedUserId(String authHeader) {
        return getAuthenticatedUser(authHeader).getId();
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody OrderRequest request) {
        User user = getAuthenticatedUser(authHeader);
        Order order = orderService.createOrder(user.getId(), request);

        // Gửi mail confirm đơn hàng
        try {
            orderMailService.sendOrderConfirmation(user, order);
        } catch (Exception e) {
            log.error("Gửi mail đơn hàng thất bại", e); // in full stack
        }

        return ResponseEntity.ok(Map.of(
                "orderId",         order.getId(),
                "status",          order.getStatus(),
                "totalAmount",     order.getTotalAmount(),
                "discountAmount",  nvl(order.getDiscountAmount()),
                "shippingFee",     asMoney(order.getShippingFee()),
                "payable",         payableOf(order)
        ));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getAuthenticatedUser(authHeader);

            Optional<Order> orderOpt = orderRepository.findByIdAndUserId(id, user.getId());
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy đơn hàng"));
            }
            Order o = orderOpt.get();
            return ResponseEntity.ok(Map.of(
                    "order",   o,
                    "payable", payableOf(o)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getAuthenticatedUser(authHeader);
            return ResponseEntity.ok(orderService.getAllOrdersByUserId(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Huỷ đơn hàng (user-side)
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

        // set reason/note trên entity đang managed
        order.setCancelReason(reason.toUpperCase());
        if (!note.isEmpty()) {
            order.setNote(note);
        }

        // đổi trạng thái qua service để hoàn kho + xoá revenue đúng chuẩn
        orderService.updateOrderStatus(order.getId(), "CANCELLED");

        return ResponseEntity.ok(Map.of(
                "message", "Đơn hàng đã được huỷ thành công",
                "orderId", order.getId(),
                "reason",  order.getCancelReason()
        ));
    }

    // Áp dụng mã giảm giá (preview hoặc ghi vào đơn PENDING)
    @PostMapping("/voucher/apply")
    public ResponseEntity<?> applyVoucher(
            @RequestBody VoucherApplyRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = getAuthenticatedUserId(authHeader);

        // Có orderId => ghi thẳng vào đơn PENDING
        if (request.getOrderId() != null) {
            var res = orderService.applyVoucherToOrder(userId, request.getOrderId(), request.getCode());
            return ResponseEntity.ok(res); // {orderId, voucherCode, discountAmount}
        }


        // Không có orderId => chỉ preview theo totalAmount client gửi
        var preview = orderService.previewVoucherDiscount(
                request.getCode(),
                request.getTotalAmount()
        );
        return ResponseEntity.ok(Map.of(
                "voucherCode", request.getCode(),
                "discountAmount", preview
        ));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateOrder(@PathVariable Long id,
                                         @RequestHeader("Authorization") String authHeader,
                                         @RequestBody UpdateOrderRequest req) {
        User user = getAuthenticatedUser(authHeader);

        Order updated = orderService.updateOrderEditableFields(user.getId(), id, req);

        return ResponseEntity.ok(Map.of(
                "orderId",        updated.getId(),
                "status",         updated.getStatus(),
                "paymentMethod",  updated.getPaymentMethod(),
                "shippingFee",    asMoney(updated.getShippingFee()),
                "note",           updated.getNote(),
                "totalAmount",    updated.getTotalAmount(),
                "discountAmount", nvl(updated.getDiscountAmount()),
                "payable",        payableOf(updated)
        ));
    }

    @GetMapping("/order")
    public ResponseEntity<?> getOrdersOrDraft(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long orderId
    ) {
        User user = getAuthenticatedUser(authHeader);

        if (orderId != null) {
            // load 1 order cụ thể
            Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn"));

            return ResponseEntity.ok(Map.of(
                    "order",   order,
                    "payable", payableOf(order)
            ));
        }

        // nếu không có param → trả list tất cả đơn
        return ResponseEntity.ok(orderService.getAllOrdersByUserId(user.getId()));
    }

}
