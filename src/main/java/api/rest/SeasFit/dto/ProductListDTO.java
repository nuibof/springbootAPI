package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    /** MỚI: min final price sau sale */
    private BigDecimal finalPrice;

    /** MỚI: có sale hay không */
    private boolean onSale;
    private List<ColorDTO> colors;
    private List<SizeDTO> sizes;
}
