package api.rest.SeasFit.controller;

import api.rest.SeasFit.component.MomoPaymentService;
import api.rest.SeasFit.component.MomoProperties;
import api.rest.SeasFit.dto.MomoIpnRequest;
import api.rest.SeasFit.dto.MomoResponse;
import api.rest.SeasFit.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment/momo")
public class MomoController {

    private final MomoProperties momo;
    private final MomoPaymentService momoPaymentService;
    private final OrderService orderService;

    // ✅ Tạo URL thanh toán MoMo từ orderId + amount
    @GetMapping("/create")
    public ResponseEntity<?> create(@RequestParam String orderId, @RequestParam String amount,@RequestParam String shippingFee) {
        long total = Long.parseLong(amount);
        int shippingFeeInt = Integer.parseInt(shippingFee);

        MomoResponse response = momoPaymentService.createMomoPayment(orderId, total, shippingFeeInt);
        return ResponseEntity.ok(response);
    }

    // ✅ Xử lý IPN callback từ MoMo
    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> handleIpn(@RequestBody MomoIpnRequest req) {
        try {
            // ✅ 1. Xây dựng raw hash theo đúng thứ tự MoMo yêu cầu
            String rawHash = "accessKey=" + momo.getAccessKey() +
                    "&amount=" + req.getAmount() +
                    "&extraData=" + req.getExtraData() +
                    "&message=" + req.getMessage() +
                    "&orderId=" + req.getOrderId() +
                    "&orderInfo=" + req.getOrderInfo() +
                    "&orderType=" + req.getOrderType() +
                    "&partnerCode=" + req.getPartnerCode() +
                    "&payType=" + req.getPayType() +
                    "&requestId=" + req.getRequestId() +
                    "&responseTime=" + req.getResponseTime() +
                    "&resultCode=" + req.getResultCode() +
                    "&transId=" + req.getTransId();

            // ✅ 2. Tính lại signature
            String mySignature = momoPaymentService.hmacSHA256(rawHash, momo.getSecretKey());

            // ✅ 3. So sánh signature
            if (!mySignature.equals(req.getSignature())) {
                return ResponseEntity.status(400).body(Map.of(
                        "message", "Invalid signature",
                        "resultCode", 1
                ));
            }

            // ✅ 4. Xác nhận thanh toán thành công
            if (req.getResultCode() == 0) {
                orderService.markAsPaid(req.getOrderId(), req.getTransId());
                System.out.println("✅ Thanh toán thành công: orderId = " + req.getOrderId());
            } else {
                System.out.println("❌ Thanh toán thất bại: " + req.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "IPN processed",
                    "resultCode", 0
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Internal server error",
                    "resultCode", 1
            ));
        }
    }
}
