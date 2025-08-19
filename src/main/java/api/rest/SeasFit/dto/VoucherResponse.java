package api.rest.SeasFit.dto;

import api.rest.SeasFit.entity.Voucher;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VoucherResponse {
    private Long id;
    private String code;
    private Double discountValue;
    private String discountType;
    private String description;
    private Double minOrderAmount;
    private Integer quantity;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;

    public static VoucherResponse fromEntity(Voucher entity) {
        VoucherResponse dto = new VoucherResponse();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setDiscountValue(entity.getDiscountValue().doubleValue());
        dto.setDiscountType(entity.getDiscountType());
        dto.setMinOrderAmount(entity.getMinOrderAmount().doubleValue());
        dto.setQuantity(entity.getQuantity());
        dto.setIsActive(entity.getIsActive());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        return dto;
    }
}
