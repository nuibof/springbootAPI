package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponseDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String status;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private Integer shippingFee;
    private String voucherCode;
    private BigDecimal discountAmount;
}
