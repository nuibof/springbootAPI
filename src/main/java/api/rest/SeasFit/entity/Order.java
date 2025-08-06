package api.rest.SeasFit.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "[order]") // Sửa lại nếu bảng thực tế tên là "orders"
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với người dùng
    @Column(nullable = false)
    private Long userId;

    // Địa chỉ đã chọn tại thời điểm đặt hàng
    @Column(nullable = false)
    private Long addressId;

    // Tổng tiền (đã bao gồm phí ship nếu có)
    @Column(nullable = false)
    private Integer totalAmount;

    // Phương thức thanh toán (cod, momo, vnpay)
    @Column(nullable = false, length = 50)
    private String paymentMethod;

    // Trạng thái đơn hàng (pending, paid, cancelled, shipped, delivered)
    @Column(nullable = false, length = 20)
    private String status;

    // ID giao dịch thanh toán (ví dụ Momo)
    private String paymentTransactionId;

    // Mã voucher nếu có
    private String voucherCode;

    // Phí vận chuyển
    private Integer shippingFee;

    // Ghi chú của khách
    @Column(columnDefinition = "NVARCHAR(255)")
    private String note;

    // Ngày đặt hàng
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Ngày cập nhật trạng thái
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
