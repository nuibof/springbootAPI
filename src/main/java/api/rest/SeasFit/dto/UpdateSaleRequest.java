// src/main/java/api/rest/SeasFit/dto/UpdateSaleRequest.java
package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateSaleRequest {
    private BigDecimal saleAmount;      // >= 0
    private LocalDateTime saleFrom;     // nullable
    private LocalDateTime saleTo;       // nullable
    private boolean clear;              // true -> reset sale
}
