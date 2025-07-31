package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);


    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double avgRatingByProductId(@Param("productId") Long productId);

    void deleteByProductId(Long productId);
}
