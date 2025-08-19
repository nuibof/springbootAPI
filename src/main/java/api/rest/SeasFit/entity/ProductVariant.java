package api.rest.SeasFit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_variant")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@SQLDelete(sql = "UPDATE product_variant SET deleted = 1 WHERE id=?")
// Ẩn biến thể đã xóa mềm khỏi mọi query mặc định
@Where(clause = "deleted = 0")
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

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    private Integer quantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // cờ soft-delete
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    // cờ hiển thị/bán (tùy dùng UI)
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Version
    private Long version;
}
