package api.rest.SeasFit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserVoucherDTO {
    private Long id;                 // id bản ghi user_voucher
    private String code;             // v.code
    private String description;      // v.description
    private String discountType;     // "fixed" | "percent"
    private BigDecimal discountValue;// giá trị giảm
    private BigDecimal minOrderAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private BigDecimal maxDiscountValue;
    // có thể add thêm assignedAt nếu bảng user_voucher có
}
