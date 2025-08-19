package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long id, Long id1);

    List<Order> findAllByUserId(Long id);
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
}
