// api.rest.SeasFit.dto.UpdateOrderRequest
package api.rest.SeasFit.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateOrderRequest {
    private Long addressId;         // đổi địa chỉ nhận (thuộc user)
    private String paymentMethod;   // COD/VNPAY/MOMO/BANKING...
    private BigDecimal shippingFee; // >= 0
    private String note;            // ghi chú
    private String voucherCode;
}
