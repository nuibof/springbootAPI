package api.rest.SeasFit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_variant")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@SQLDelete(sql = "UPDATE product_variant SET deleted = 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted = 0")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "size_id")
    private Size size;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // soft-delete flag
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    // hiển thị/bán
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // ====== SALE (giảm VND tuyệt đối) ======
    @Column(name = "sale_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal saleAmount = BigDecimal.ZERO;     // mặc định 0

    @Column(name = "sale_from")
    private LocalDateTime saleFrom;                      // null = bắt đầu ngay

    @Column(name = "sale_to")
    private LocalDateTime saleTo;                        // null = không hết hạn

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.saleAmount == null) this.saleAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        if (this.saleAmount == null) this.saleAmount = BigDecimal.ZERO;
    }

    @Version
    private Long version;

    // ====== Helpers không lưu DB ======
    @Transient
    public boolean isSaleActiveNow() {
        if (saleAmount == null || saleAmount.signum() <= 0) return false;
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = (saleFrom == null) || !now.isBefore(saleFrom); // now >= from
        boolean beforeEnd  = (saleTo == null)   || !now.isAfter(saleTo);    // now <= to
        return afterStart && beforeEnd;
    }

    /** Giá thực bán (clamp >= 0) */
    @Transient
    public BigDecimal getEffectivePrice() {
        BigDecimal base = (price != null) ? price : BigDecimal.ZERO;
        if (!isSaleActiveNow()) return base;

        BigDecimal discount = saleAmount.max(BigDecimal.ZERO);
        BigDecimal effective = base.subtract(discount);
        return (effective.signum() < 0) ? BigDecimal.ZERO : effective;
    }
}
