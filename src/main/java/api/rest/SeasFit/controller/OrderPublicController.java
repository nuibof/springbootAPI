// src/main/java/api/rest/SeasFit/controller/OrderPublicController.java
package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.OrderAddress;
import api.rest.SeasFit.repository.OrderAddressRepository;
import api.rest.SeasFit.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/public")
public class OrderPublicController {

    private final OrderRepository orderRepository;
    private final OrderAddressRepository orderAddressRepository;

    @GetMapping
    public ResponseEntity<?> getOrderPublic(
            @RequestParam("orderId") Long orderId,
            @RequestParam("key") long key
    ) {
        // 1) Lấy order (header)
        Order o = orderRepository.findById(orderId).orElse(null);
        if (o == null) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));

        // 2) Verify key = createdAt (ms)
        long createdMs = o.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (createdMs != key) return ResponseEntity.status(401).body(Map.of("error", "Key không hợp lệ"));

        // 3) Address theo order_id (nếu có)
        OrderAddress addr = o.getAddressId() != null
                ? orderAddressRepository.findById(o.getAddressId()).orElse(null)
                : null;

        Map<String,Object> addressDto = null;
        if (addr != null) {
            addressDto = new LinkedHashMap<>();
            addressDto.put("street",   nvl(addr.getStreet()));
            addressDto.put("ward",     nvl(addr.getWard()));
            addressDto.put("district", nvl(addr.getDistrict()));
            addressDto.put("city",     nvl(addr.getCity()));
            addressDto.put("country",  nvl(addr.getCountry()));
        }

        // 4) Items: dùng projection native để bypass lazy/where
        List<OrderRepository.OrderItemFlat> flats =
                orderRepository.findFlatByOrderIdsIncludingSoftDeleted(List.of(orderId));

        List<Map<String,Object>> itemDtos = flats.stream().map(f -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("quantity",      f.getQuantity());
            m.put("price",         f.getPrice());          // đơn giá sau sale
            m.put("originalPrice", null);                  // nếu BE không lưu, giữ null
            m.put("productName",   f.getProductName());
            m.put("colorName",     f.getColorName());
            m.put("sizeLabel",     f.getSizeLabel());
            m.put("imageUrl",      f.getImageUrl());       // alias AS ImageUrl trong query
            return m;
        }).toList();

        // 5) Payable
        BigDecimal total = n(o.getTotalAmount());
        BigDecimal ship  = n(o.getShippingFee());
        BigDecimal disc  = n(o.getDiscountAmount());
        BigDecimal payable = total.add(ship).subtract(disc);
        if (payable.signum() < 0) payable = BigDecimal.ZERO;

        // 6) DTO gọn trả cho FE
        Map<String,Object> orderDto = new LinkedHashMap<>();
        orderDto.put("id",             o.getId());
        orderDto.put("status",         o.getStatus());
        orderDto.put("createdAt",      o.getCreatedAt());
        orderDto.put("paymentMethod",  o.getPaymentMethod());
        orderDto.put("voucherCode",    o.getVoucherCode());
        orderDto.put("note",           o.getNote());
        orderDto.put("totalAmount",    o.getTotalAmount());
        orderDto.put("discountAmount", o.getDiscountAmount());
        orderDto.put("shippingFee",    o.getShippingFee());
        orderDto.put("payable",        payable);
        orderDto.put("receiverName",   addr != null ? nvl(addr.getFullName()) : "");
        orderDto.put("receiverPhone",  addr != null ? nvl(addr.getPhone())    : "");
        orderDto.put("address",        addressDto);
        orderDto.put("items",          itemDtos);

        return ResponseEntity.ok(Map.of("order", orderDto));
    }

    /* ===== helpers ===== */
    private static BigDecimal n(Number x) {
        if (x == null) return BigDecimal.ZERO;
        if (x instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(x.longValue());
    }
    private static String nvl(Object s) { return s == null ? "" : String.valueOf(s); }
}
