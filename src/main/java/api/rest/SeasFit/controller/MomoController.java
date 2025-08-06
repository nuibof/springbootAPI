package api.rest.SeasFit.controller;

import api.rest.SeasFit.component.MomoPaymentService;
import api.rest.SeasFit.component.MomoProperties;
import api.rest.SeasFit.dto.MomoIpnRequest;
import api.rest.SeasFit.dto.MomoResponse;
import api.rest.SeasFit.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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

    @GetMapping("/create")
    public ResponseEntity<?> create(@RequestParam String orderId, @RequestParam String amount) {
        MomoResponse response = momoPaymentService.createMomoPayment(orderId, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> handleIpn(@RequestBody MomoIpnRequest req) {

        if (req.getResultCode() == 0) {
            orderService.markAsPaid(req.getOrderId(), req.getTransId());
        }
        try {
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

            String mySignature = momoPaymentService.hmacSHA256(rawHash, momo.getSecretKey());

            if (!mySignature.equals(req.getSignature())) {
                return ResponseEntity.status(400).body(Map.of(
                        "message", "invalid signature",
                        "resultCode", 1
                ));
            }

            if (req.getResultCode() == 0) {
                System.out.println("✅ Thanh toán thành công cho orderId: " + req.getOrderId());
            } else {
                System.out.println("❌ Thanh toán thất bại: " + req.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "success",
                    "resultCode", 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "error",
                    "resultCode", 1
            ));
        }

    }
}
