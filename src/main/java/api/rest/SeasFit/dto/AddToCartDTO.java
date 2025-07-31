package api.rest.SeasFit.dto;

import lombok.Data;

@Data
public class AddToCartDTO {
    private Long productId;
    private Long colorId;
    private Long sizeId;
    private int quantity;
}
