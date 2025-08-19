package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherApplyRequest {
    private String code;
    private BigDecimal totalAmount;
}
