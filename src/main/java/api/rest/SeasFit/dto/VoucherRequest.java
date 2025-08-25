package api.rest.SeasFit.dto;

import lombok.Data;

@Data
public class VoucherRequest {
    private String code;
    private Double discountValue;
    private String discountType;
    private Double minOrderAmount;
    private String description;
    private Integer quantity;
    private Integer maxDiscountValue; // optional, can be null
    private String startDate; // format yyyy-MM-dd
    private String endDate;
    private Boolean isActive;
}
