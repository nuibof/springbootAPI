// api/rest/SeasFit/entity/UserVoucher.java
package api.rest.SeasFit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers",
       uniqueConstraints = @UniqueConstraint(name="uq_uv_user_code", columnNames = {"user_id","voucher_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserVoucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Giả định Voucher có field `code` unique (thường bạn đã có)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_code", referencedColumnName = "code", nullable = false)
    private Voucher voucher;
}
