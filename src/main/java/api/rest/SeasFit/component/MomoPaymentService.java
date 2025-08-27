package api.rest.SeasFit.component;

import api.rest.SeasFit.dto.MomoResponse;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.repository.VoucherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MomoPaymentService {

    private final String partnerCode = "MOMO";
    private final String accessKey   = "F8BBA842ECF85";
    private final String secretKey   = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String endpoint    = "https://test-payment.momo.vn/v2/gateway/api/create";

    @Value("${backend.url}")
    private String backendUrl;   // ví dụ: https://api.seasfit.vn
    @Value("${frontend.url}")
    private String frontendUrl;  // ví dụ: https://seasfit.vn

    @Autowired private OrderRepository orderRepository;
    @Autowired private VoucherRepository voucherRepository;

    public MomoResponse createMomoPayment(String orderIdStr, long totalAmount, int shippingFee) {
        // --- Build URL SAU KHI @Value đã inject ---
        final String ipnUrl       = backendUrl + "/api/payment/momo/ipn";
        final String redirectBase = frontendUrl + "/account/orders";
        final String returnUrlFull = redirectBase + "?orderId=" + orderIdStr;

        String requestId   = UUID.randomUUID().toString();
        String requestType = "captureWallet";
        String momoOrderId = orderIdStr + "-" + System.currentTimeMillis();
        String orderInfo   = "Thanh toán đơn hàng " + orderIdStr;

        long orderId = Long.parseLong(orderIdStr);
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return null;

        // ======= TÍNH GIẢM GIÁ =======
        BigDecimal total     = BigDecimal.valueOf(totalAmount);
        BigDecimal discount  = BigDecimal.ZERO;

        // Ưu tiên dùng field code string lưu trên order; nếu không thì lấy từ entity Voucher
        String voucherCode = order.getVoucherCode();
        if ((voucherCode == null || voucherCode.isBlank()) && order.getVoucher() != null) {
            voucherCode = order.getVoucher().getCode(); // <-- dùng đúng code
        }

        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherRepository.findByCode(voucherCode).ifPresent(v -> {
                BigDecimal d = BigDecimal.ZERO;
                if ("fixed".equalsIgnoreCase(v.getDiscountType())) {
                    d = v.getDiscountValue();
                } else if ("percent".equalsIgnoreCase(v.getDiscountType())) {
                    // percent value: ví dụ 15 → 15%
                    d = total.multiply(
                            v.getDiscountValue()
                                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
                    );
                }
                // cap discount không vượt tổng
                if (d.compareTo(total) > 0) d = total;
                // gán ra ngoài
                // (trick: dùng wrapper hoặc AtomicReference, hoặc trả về qua biến final[0])
                // Ở đây viết trực tiếp cho gọn
            });
            // Nếu muốn discount “ra ngoài” block ifPresent, hãy refactor dùng biến AtomicReference<BigDecimal>
        }

        // Nếu dùng ifPresent như trên, refactor lại:
        if (voucherCode != null && !voucherCode.isBlank()) {
            var optional = voucherRepository.findByCode(voucherCode);
            if (optional.isPresent()) {
                var v = optional.get();
                if ("fixed".equalsIgnoreCase(v.getDiscountType())) {
                    discount = v.getDiscountValue();
                } else if ("percent".equalsIgnoreCase(v.getDiscountType())) {
                    discount = total.multiply(
                            v.getDiscountValue()
                                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
                    );
                }
                if (discount.compareTo(total) > 0) discount = total;
            }
        }

        BigDecimal payable = total.subtract(discount).add(BigDecimal.valueOf(shippingFee));
        if (payable.signum() < 0) payable = BigDecimal.ZERO;

        String amountStr = payable.toBigInteger().toString();

        // extraData: nên base64 để tránh ký tự lạ
        String rawExtra = orderIdStr + "|fee:" + shippingFee + "|discount:" + discount.toPlainString();
        String extraData = Base64.getEncoder().encodeToString(rawExtra.getBytes(StandardCharsets.UTF_8));

        try {
            // ---- RAW SIGNATURE (đúng thứ tự theo MoMo docs) ----
            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amountStr
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + ipnUrl
                    + "&orderId=" + momoOrderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + partnerCode
                    + "&redirectUrl=" + returnUrlFull
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;

            String signature = hmacSHA256(rawSignature, secretKey);

            Map<String, String> body = new LinkedHashMap<>();
            body.put("partnerCode", partnerCode);
            body.put("accessKey", accessKey);
            body.put("requestId", requestId);
            body.put("amount", amountStr);
            body.put("orderId", momoOrderId);
            body.put("orderInfo", orderInfo);
            body.put("redirectUrl", returnUrlFull);
            body.put("ipnUrl", ipnUrl);
            body.put("extraData", extraData);
            body.put("requestType", requestType);
            body.put("signature", signature);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, req, String.class);
            System.out.println("MoMo RAW Response: " + response.getBody());

            return new ObjectMapper().readValue(response.getBody(), MomoResponse.class);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi tạo giao dịch MoMo: " + e.getMessage());
            return null;
        }
    }

    public String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
