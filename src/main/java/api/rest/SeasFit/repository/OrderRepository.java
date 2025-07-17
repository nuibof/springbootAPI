package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
