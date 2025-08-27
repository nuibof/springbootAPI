package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAdminDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String category;
    private String gender;
    private String status;
    private LocalDateTime createdAt;
    private int variantCount;
    private int totalQuantity;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal effectiveMinPrice;
    private BigDecimal effectiveMaxPrice;
}
