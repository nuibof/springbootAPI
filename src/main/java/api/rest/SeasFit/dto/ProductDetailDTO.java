package api.rest.SeasFit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;

    // ===== Tổng hợp giá ở cấp sản phẩm =====
    /** Giá gốc rẻ nhất trong các biến thể */
    private BigDecimal price;            // giữ nguyên trường cũ (min original price)
    /** Giá phải trả rẻ nhất (đã trừ sale_amount, không âm) */
    private BigDecimal finalPrice;       // NEW
    /** Sản phẩm đang có ít nhất một biến thể giảm giá trong khung thời gian hợp lệ */
    private boolean onSale;              // NEW

    private List<ColorDTO> colors;
    private int favorites;
    private Double rating;
    private List<ReviewDTO> reviews;
    private String status;

    @Data
    public static class ColorDTO {
        private int id;
        private String name;
        private String hex;
        private String image;

        // ===== Tổng hợp giá theo màu =====
        /** Giá gốc rẻ nhất của màu này */
        private BigDecimal minPrice;       // NEW
        /** Giá phải trả rẻ nhất của màu này */
        private BigDecimal minFinalPrice;  // NEW
        /** Màu này có biến thể đang sale hay không */
        private boolean onSale;            // NEW

        private List<SizeDTO> sizes;
    }

    @Data
    public static class SizeDTO {
        private int id;
        private String label;
        private int quantity;

        // ===== Giá theo biến thể (màu + size) =====
        /** Giá gốc của biến thể */
        private BigDecimal price;          // NEW
        /** Mức giảm theo VND (sale_amount) */
        private BigDecimal saleAmount;     // NEW
        /** Giá phải trả = max(price - saleAmount, 0) */
        private BigDecimal finalPrice;     // NEW

        /** Khung thời gian áp dụng giảm giá (có thể null) */
        private LocalDateTime saleFrom;    // NEW
        private LocalDateTime saleTo;      // NEW

        /** Biến thể này có đang nằm trong thời gian sale hợp lệ không */
        private boolean onSale;            // NEW
    }

    @Data
    public static class ReviewDTO {
        private Long id;
        private String content;
        private int rating;
        private String userName;
        private LocalDateTime createdAt;
    }
}
