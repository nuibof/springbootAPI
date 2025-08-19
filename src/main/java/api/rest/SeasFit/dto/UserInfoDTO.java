package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {
    private Long id;
    private String userName;
    private String fullName;
    private String email;
    private String phone;
    private String identityCard;
    private String imageUrl;
    private Boolean gender;        // true = nam, false = nữ (hoặc bạn define enum)
    private String role;
    private String dateOfBirth;    // yyyy-MM-dd
    private String createdAt;      // để hiển thị thông tin
}
