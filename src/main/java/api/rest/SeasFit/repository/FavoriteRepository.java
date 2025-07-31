package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    int countByProductId(Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteByProductId(Long productId);
}
