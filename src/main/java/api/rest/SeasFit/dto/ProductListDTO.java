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
    private List<ColorDTO> colors;
    private List<SizeDTO> sizes;
}
