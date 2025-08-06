package api.rest.SeasFit.component;

import api.rest.SeasFit.dto.MomoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MomoPaymentService {

    public MomoResponse createMomoPayment(String orderId, String amount) {
        String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
        String partnerCode = "MOMO";
        String accessKey = "F8BBA842ECF85";
        String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
        String requestId = UUID.randomUUID().toString();
        String requestType = "captureWallet";
        String momoOrderId = orderId + "-" + System.currentTimeMillis(); // unique
        String orderInfo = "Thanh toán đơn hàng " + orderId;
        String returnUrl = "http://192.168.100.239:5173/checkout-success?orderId=" + orderId;
        String notifyUrl = "http://192.168.100.239:8080/api/payment/momo/ipn";
        String extraData = orderId; // gửi kèm ID gốc

        try {
            // ✅ Build raw signature đúng thứ tự
            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + notifyUrl
                    + "&orderId=" + momoOrderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + partnerCode
                    + "&redirectUrl=" + returnUrl
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;

            String signature = hmacSHA256(rawSignature, secretKey);

            // ✅ Build payload JSON
            Map<String, String> rawData = new LinkedHashMap<>();
            rawData.put("accessKey", accessKey);
            rawData.put("amount", amount);
            rawData.put("extraData", extraData);
            rawData.put("ipnUrl", notifyUrl);
            rawData.put("orderId", momoOrderId);
            rawData.put("orderInfo", orderInfo);
            rawData.put("partnerCode", partnerCode);
            rawData.put("redirectUrl", returnUrl);
            rawData.put("requestId", requestId);
            rawData.put("requestType", requestType);
            rawData.put("signature", signature);

            // ✅ Gửi request
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
