package api.rest.SeasFit.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long addressId;
    private String paymentMethod;
    private List<OrderItemRequest> items;
}