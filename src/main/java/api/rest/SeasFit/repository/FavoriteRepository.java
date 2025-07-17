package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Custom query to find favorites by user ID
    List<Favorite> findByUserId(Long userId);

    // Custom query to find a favorite by user ID and product ID
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    // Count by product ID
    int countByProductId(Long productId);
}
