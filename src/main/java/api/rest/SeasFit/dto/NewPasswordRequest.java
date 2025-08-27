// src/main/java/api/rest/SeasFit/dto/NewPasswordRequest.java
package api.rest.SeasFit.dto;
import lombok.Data;
@Data public class NewPasswordRequest { private String email; private String otp; private String newPassword; }