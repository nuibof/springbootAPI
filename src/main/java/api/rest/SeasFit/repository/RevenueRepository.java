package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {
    boolean existsByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
