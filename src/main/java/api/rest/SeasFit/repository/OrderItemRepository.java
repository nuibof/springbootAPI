package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // -------- Basic ----------
    List<OrderItem> findByOrderId(Long orderId);

    // Lưu ý: nếu ProductVariant có @Where, 2 method dưới có thể trả sai khi pv bị soft delete
    boolean existsByProductVariant_Id(Long id);
    boolean existsByProductVariant_Product_Id(Long productId);

    // -------- Native: bypass @Where (dùng khi cần tính đúng kể cả pv đã soft delete) ----------
    @Query(value = """
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
        FROM order_item oi
        WHERE oi.product_variant_id = :variantId
        """, nativeQuery = true)
    boolean existsByProductVariantIdIncludingSoftDeleted(@Param("variantId") Long variantId);

    @Query(value = """
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
        FROM order_item oi
        JOIN product_variant pv ON pv.id = oi.product_variant_id
        WHERE pv.product_id = :productId
        """, nativeQuery = true)
    boolean existsByProductIdIncludingSoftDeleted(@Param("productId") Long productId);

    // Lấy items + JOIN pv kể cả soft delete (nếu cần map entity vẫn ổn vì select oi.*)
    @Query(value = """
        SELECT oi.*
        FROM order_item oi
        LEFT JOIN product_variant pv ON pv.id = oi.product_variant_id
        WHERE oi.order_id = :orderId
        ORDER BY oi.id ASC
        """, nativeQuery = true)
    List<OrderItem> findByOrderIdIncludingSoftDeleted(@Param("orderId") Long orderId);

    // Optional: đếm số item theo order (bypass @Where hoàn toàn)
    @Query(value = """
        SELECT COUNT(1)
        FROM order_item oi
        WHERE oi.order_id = :orderId
        """, nativeQuery = true)
    long countItemsByOrderIdNative(@Param("orderId") Long orderId);
}
