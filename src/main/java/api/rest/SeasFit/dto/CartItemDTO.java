package api.rest.SeasFit.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private int quantity;
    private Integer colorId;
    private String colorName;
    private String colorHex;
    private Integer sizeId;
    private String sizeLabel;
}
