package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Custom query to find reviews by product ID
    List<Review> findByProductId(Long productId);

    // Custom query to find reviews by user ID
    List<Review> findByUserId(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double avgRatingByProductId(@Param("productId") Long productId);

}
