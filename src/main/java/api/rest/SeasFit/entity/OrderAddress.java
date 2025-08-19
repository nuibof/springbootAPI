package api.rest.SeasFit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_address",
       uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;  // 1-1 với đơn hàng

  @Column(name = "full_name", length = 100, nullable = false)
  private String fullName;

  @Column(name = "phone", length = 15, nullable = false)
  private String phone;

  @Column(name = "street", length = 255)
  private String street;

  @Column(name = "ward", length = 100)
  private String ward;

  @Column(name = "district", length = 100)
  private String district;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "country", length = 100)
  private String country;
}
