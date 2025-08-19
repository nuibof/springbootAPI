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

    String partnerCode = "MOMO";
    String accessKey = "F8BBA842ECF85";
    String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private final String ipnUrl = "http://192.168.100.239:8080/api/payment/momo/ipn"; // sửa theo domain backend bạn
    private final String redirectUrl = "http://192.168.100.239:5173/checkout-success"; // sửa theo frontend bạn

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    public MomoResponse createMomoPayment(String orderIdStr, long totalAmount, int shippingFee) {
        String requestId = UUID.randomUUID().toString();
        String requestType = "captureWallet";
        String momoOrderId = orderIdStr + "-" + System.currentTimeMillis();
        String orderInfo = "Thanh toán đơn hàng " + orderIdStr;
        String returnUrlFull = redirectUrl + "?orderId=" + orderIdStr;

        long orderId = Long.parseLong(orderIdStr);
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return null;

        Order order = orderOpt.get();

        BigDecimal discount = BigDecimal.ZERO;

        if (order.getVoucher() != null) {
            Optional<Voucher> optional = voucherRepository.findByCode(String.valueOf(order.getVoucher()));

            if (optional.isPresent()) {
                Voucher voucher = optional.get();
                if (voucher.getDiscountType().equals("fixed")) {
                    discount = voucher.getDiscountValue();
                } else if (voucher.getDiscountType().equals("percent")) {
                    discount = BigDecimal.valueOf(totalAmount)
                            .multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));
                }
            }
        }


        long discountValue = discount.longValue();
        long totalWithShipping = totalAmount - discountValue + shippingFee;

        String amountStr = String.valueOf(totalWithShipping);
        String extraData = orderIdStr + "|fee:" + shippingFee + "|discount:" + discountValue;

        try {
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

            Map<String, String> rawData = new LinkedHashMap<>();
            rawData.put("accessKey", accessKey);
            rawData.put("amount", amountStr);
            rawData.put("extraData", extraData);
            rawData.put("ipnUrl", ipnUrl);
            rawData.put("orderId", momoOrderId);
            rawData.put("orderInfo", orderInfo);
            rawData.put("partnerCode", partnerCode);
            rawData.put("redirectUrl", returnUrlFull);
            rawData.put("requestId", requestId);
            rawData.put("requestType", requestType);
            rawData.put("signature", signature);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(rawData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);
            System.out.println("MoMo RAW Response: " + response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getBody(), MomoResponse.class);
        } catch (Exception e) {
            System.out.println("Lỗi khi tạo giao dịch MoMo: " + e.getMessage());
            return null;
        }
    }


    public String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
