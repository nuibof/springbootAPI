package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAdminDTO {
    private Long id;
    private String status;
    private String paymentMethod;
    private String voucherCode;
    private String note;
    private Integer totalAmount;
    private BigDecimal discountAmount;
    private int shippingFee;
    private String cancelReason;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
