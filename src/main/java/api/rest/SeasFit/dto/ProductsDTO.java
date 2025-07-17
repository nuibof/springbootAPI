package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductsDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private Double rating;
}
