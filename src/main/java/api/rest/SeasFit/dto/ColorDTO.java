package api.rest.SeasFit.dto;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorDTO {
    private Integer id;
    private String name;
    private String hexCode;
    private String imagePreview;
    private BigDecimal price;
    private List<SizeDTO> sizes;
}


