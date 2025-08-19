package api.rest.SeasFit.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long addressId;
    private String note;
    private String paymentMethod;
    private int shippingFee;
    private String voucherCode;
    private Integer discountAmount;
    private List<OrderItemRequest> items;
}