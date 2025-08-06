package api.rest.SeasFit.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private Long colorId;
    private Long sizeId;
    private int quantity;
    private int price;
}