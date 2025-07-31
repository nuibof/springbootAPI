package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductInputDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private int categoryId;
    private String status;
    private Integer gender;
    private List<ColorVariantDTO> colors;

    @Data
    public static class ColorVariantDTO {
        private int id;
        private String image;
        private List<String> sizes;
    }
}
