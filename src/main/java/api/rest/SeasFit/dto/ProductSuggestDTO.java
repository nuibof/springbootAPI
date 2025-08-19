package api.rest.SeasFit.dto;

import java.math.BigDecimal;

public record ProductSuggestDTO(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl
) {}