package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByIdAndUserId(Long id, Long id1);
    List<Order> findAllByUserId(Long id);

    // JPQL cũ: có thể bị @Where chặn soft-delete
    @Query("""
       select distinct o from Order o
       left join fetch o.items oi
       left join fetch oi.productVariant pv
       left join fetch pv.product p
       left join fetch pv.color c
       left join fetch pv.size s
       where o.userId = :userId
       order by o.createdAt desc
    """)
    List<Order> findAllWithItemsByUserId(@Param("userId") Long userId);

    Long countByUserId(Long id);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    // ================== GỌN – BYPASS @Where, KHÔNG TẠO FILE MỚI ==================
    // Projection NHÉT NGAY TRONG REPO
    interface OrderItemFlat {
        Long getOrderId();
        String getStatus();
        String getPaymentMethod();
        String getVoucherCode();
        String getNote();
        BigDecimal getTotalAmount();
        BigDecimal getDiscountAmount();
        BigDecimal getShippingFee();
        String getCancelReason();
        Instant getCreatedAt();

        Integer getQuantity();
        BigDecimal getPrice();

        String getProductName();
        String getColorName();
        String getSizeLabel();
    }

    // Tất cả order (hoặc filter theo user nếu truyền userId)
    @Query(value = """
    SELECT
      o.id                 AS orderId,
      o.status             AS status,
      o.payment_method     AS paymentMethod,
      o.voucher_code            AS voucherCode,
      o.note               AS note,
      o.total_amount       AS totalAmount,
      o.discount_amount    AS discountAmount,
      o.shipping_fee       AS shippingFee,
      o.cancel_reason      AS cancelReason,
      o.created_at         AS createdAt,

      oi.quantity          AS quantity,
      oi.price             AS price,

      p.name               AS productName,
      c.name               AS colorName,
      s.label              AS sizeLabel
    FROM [order] o
    LEFT JOIN order_item oi        ON oi.order_id = o.id
    LEFT JOIN product_variant pv   ON pv.id = oi.variant_id
    LEFT JOIN product p            ON p.id = pv.product_id
    LEFT JOIN [color] c            ON c.id = pv.color_id
    LEFT JOIN [size] s             ON s.id = pv.size_id
    WHERE (:userId IS NULL OR o.user_id = :userId)
    ORDER BY o.created_at DESC, oi.id ASC
    """, nativeQuery = true)
    List<OrderItemFlat> findAllFlatIncludingSoftDeleted(@Param("userId") Long userId);


    // Nếu muốn bắt buộc theo userId (cho gọn)
    @Query(value = """
        SELECT
          o.id                AS orderId,
          o.status            AS status,
          o.payment_method    AS paymentMethod,
          v.code              AS voucherCode,
          o.note              AS note,
          o.total_amount      AS totalAmount,
          o.discount_amount   AS discountAmount,
          o.shipping_fee      AS shippingFee,
          o.cancel_reason     AS cancelReason,
          o.created_at        AS createdAt,

          oi.quantity         AS quantity,
          oi.price            AS price,

          p.name              AS productName,
          c.name              AS colorName,
          s.label             AS sizeLabel
        FROM [order] o
        LEFT JOIN order_item oi      ON oi.order_id = o.id
        LEFT JOIN product_variant pv ON pv.id = oi.variant_id
        LEFT JOIN product p          ON p.id = pv.product_id
        LEFT JOIN color c            ON c.id = pv.color_id
        LEFT JOIN size s             ON s.id = pv.size_id
        WHERE o.user_id = :userId
        ORDER BY o.created_at DESC, oi.id ASC
    """, nativeQuery = true)
    List<OrderItemFlat> findAllFlatIncludingSoftDeletedByUser(@Param("userId") Long userId);

    // Projection nội bộ (khỏi tạo file)
    interface OrderItemFlatUser {
        Long getOrderId();
        java.time.LocalDateTime getCreatedAt();
        String getStatus();
        String getPaymentMethod();
        java.math.BigDecimal getTotalAmount();
        java.math.BigDecimal getShippingFee();
        java.math.BigDecimal getDiscountAmount();
        String getNote();
        String getVoucherCode();

        // item
        Integer getQuantity();
        java.math.BigDecimal getPrice();

        // product/variant (có thể null nếu soft delete)
        Long getProductId();
        String getProductName();
        String getColorName();
        String getSizeLabel();

        // address (có thể null)
        String getAddrFullName();
        String getAddrPhone();
        String getAddrStreet();
        String getAddrWard();
        String getAddrDistrict();
        String getAddrCity();
        String getAddrCountry();
    }

    @Query(value = """
    SELECT
      o.id                 AS orderId,
      o.created_at         AS createdAt,      -- nếu projection là LocalDateTime thì ok
      o.status             AS status,
      o.payment_method     AS paymentMethod,
      o.total_amount       AS totalAmount,
      o.shipping_fee       AS shippingFee,
      o.discount_amount    AS discountAmount,
      o.note               AS note,
      o.voucher_code       AS voucherCode,    -- lấy trực tiếp từ orders

      oi.quantity          AS quantity,
      oi.price             AS price,

      p.id                 AS productId,
      p.name               AS productName,
      c.name               AS colorName,
      s.label              AS sizeLabel,

      oa.full_name         AS addrFullName,
      oa.phone             AS addrPhone,
      oa.street            AS addrStreet,
      oa.ward              AS addrWard,
      oa.district          AS addrDistrict,
      oa.city              AS addrCity,
      oa.country           AS addrCountry

    FROM [order] o
    LEFT JOIN order_item oi        ON oi.order_id = o.id
    LEFT JOIN product_variant pv   ON pv.id = oi.variant_id   -- BYPASS @Where
    LEFT JOIN product p            ON p.id = pv.product_id
    LEFT JOIN [color] c            ON c.id = pv.color_id
    LEFT JOIN [size] s             ON s.id = pv.size_id
    LEFT JOIN order_address oa     ON oa.order_id = o.id
    WHERE o.user_id = :userId
    ORDER BY o.created_at DESC, oi.id ASC
    """, nativeQuery = true)
    List<OrderItemFlatUser> findOrdersForUserFlat(@Param("userId") Long userId);

    // Lấy items cho đúng các orderId của 1 trang (bypass @Where)
    @Query(value = """
  SELECT
    o.id            AS orderId,
    o.status        AS status,
    o.payment_method AS paymentMethod,
    o.voucher_code  AS voucherCode,
    o.note          AS note,
    o.total_amount  AS totalAmount,
    o.discount_amount AS discountAmount,
    o.shipping_fee  AS shippingFee,
    o.cancel_reason AS cancelReason,
    o.created_at    AS createdAt,

    oi.quantity     AS quantity,
    oi.price        AS price,

    p.name          AS productName,
    c.name          AS colorName,
    s.label         AS sizeLabel
  FROM [order] o
  JOIN order_item oi        ON oi.order_id = o.id
  LEFT JOIN product_variant pv ON pv.id = oi.variant_id
  LEFT JOIN product p           ON p.id = pv.product_id
  LEFT JOIN [color] c           ON c.id = pv.color_id
  LEFT JOIN [size] s            ON s.id = pv.size_id
  WHERE o.id IN (:ids)
  ORDER BY o.created_at DESC, oi.id ASC
""", nativeQuery = true)
    List<OrderItemFlat> findFlatByOrderIdsIncludingSoftDeleted(@Param("ids") List<Long> ids);
}
