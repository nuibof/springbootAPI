package api.rest.SeasFit.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CartItemDTO {
    private Long id;

    private Long productId;
    private String productName;
    private String imageUrl;

    /** Đơn giá gốc (chưa khuyến mãi) */
    private BigDecimal price;

    /** Đơn giá sau khuyến mãi. Nếu không KM thì = price */
    private BigDecimal finalPrice;

    /** True nếu đang có KM (finalPrice < price) */
    private Boolean onSale;

    /** Số tiền giảm trên 1 đơn vị (= price - finalPrice), có thể = 0 */
    private BigDecimal saleAmount;

    private int quantity;

    private Integer colorId;
    private String colorName;
    private String colorHex;

    private Integer sizeId;
    private String sizeLabel;
}
