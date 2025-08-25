package api.rest.SeasFit.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteItemDTO {
    private Long id;                 // product id
    private String name;             // product name
    private String imageUrl;         // product main image
    private BigDecimal price;        // min original price among variants
    private BigDecimal finalPrice;   // min effective price after sale
    private boolean onSale;          // finalPrice < price ?
    private BigDecimal saleAmount;   // discount applied at the min-price variant
}
