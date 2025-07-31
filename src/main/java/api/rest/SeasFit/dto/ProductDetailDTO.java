package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private List<ColorDTO> colors;
    private int favorites;
    private Double rating;
    private List<ReviewDTO> reviews;

    @Data
    public static class ColorDTO {
        private int id;
        private String name;
        private String hex;
        private String image;
        private List<SizeDTO> sizes;
    }

    @Data
    public static class SizeDTO {
        private int id;
        private String label;
        private int quantity;
    }

    @Data
    public static class ReviewDTO {
        private Long id;
        private String content;
        private int rating;
        private String userName;
        private LocalDateTime createdAt;
    }
}
