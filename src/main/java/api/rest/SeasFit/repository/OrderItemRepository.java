package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Iterable<? extends OrderItem> findByOrderId(Long orderId);

    boolean existsByProductVariant_Id(Long id);

    boolean existsByProductVariant_Product_Id(Long productId);
}
