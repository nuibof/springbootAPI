package api.rest.SeasFit.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long addressId;
    private String paymentMethod;
    private Integer shippingFee;
    private String note;
    private String voucherCode;
    private List<OrderItemRequest> items;

    // NEW: list id của cart items được chọn
    private List<Long> cartItemIds;

    // getters/setters ...
}