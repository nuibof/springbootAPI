package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {

    private String fullName;
    private String phone;
    private Long id;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String country;
    private Boolean isDefault;
}
