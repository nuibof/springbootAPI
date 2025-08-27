// dto/AiSuggestResponse.java
package api.rest.SeasFit.dto;

import java.util.List;

public record AiSuggestResponse(
    String answer,                 // text AI trả lời
    List<ProductListDTO> items     // list sp để FE hiển thị ngay
) {}
