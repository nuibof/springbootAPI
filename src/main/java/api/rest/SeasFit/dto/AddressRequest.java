package api.rest.SeasFit.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String fullName;
    private String phone;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String country;
    private Boolean isDefault;
}
