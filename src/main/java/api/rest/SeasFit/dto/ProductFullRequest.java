package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductFullRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String status;
    private String imageUrl;
    private Integer categoryId;
    private Integer gender;
    private List<ColorRequest> colors;

    @Data
    public static class ColorRequest {
        private String name;
        private String hex;
        private String image;
        private BigDecimal price;
        private List<String> sizes;
    }
}

