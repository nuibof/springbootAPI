// dto/OrderSummaryDTO.java
package api.rest.SeasFit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class OrderSummaryDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String status;
    private String paymentMethod;
    private String voucherCode;
    private Integer totalAmount;      // tiền hàng trước giảm
    private Integer discountAmount;   // nếu có
    private Integer shippingFee;      // nếu muốn hiện
    private List<ItemDTO> items;

    @Getter @Setter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class ItemDTO {
        private Long productId;
        private String productName;
        private String colorName;
        private String sizeLabel;
        private Integer quantity;
        private BigDecimal price;     // đơn giá thời điểm đặt
        private String image;         // tuỳ chọn: ảnh theo màu/biến thể
    }
}
