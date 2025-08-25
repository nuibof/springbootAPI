package api.rest.SeasFit.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderItemRequest {
    private Long productId;
    private Long colorId;
    private Long sizeId;
    private int quantity;
    private int price;
    private List<Long> cartItemIds;
}