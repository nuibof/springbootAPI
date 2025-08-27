// dto/AiSuggestRequest.java
package api.rest.SeasFit.dto;

import java.math.BigDecimal;

public record AiSuggestRequest(
    String message,
    Long categoryId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer limit
) {}
