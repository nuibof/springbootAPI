package api.rest.SeasFit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ bỏ qua các field không cần
public class MomoResponse {
    private int resultCode;
    private String message;
    private String payUrl; // Có thể null nếu bị lỗi
}
