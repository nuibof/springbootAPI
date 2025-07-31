package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Favorite;
import api.rest.SeasFit.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    public Optional<Favorite> findById(Long id) {
        return favoriteRepository.findById(id);
    }

    public Favorite save(Favorite entity) {
        return favoriteRepository.save(entity);
    }

    public void deleteById(Long id) {
        favoriteRepository.deleteById(id);
    }

    public boolean toggleFavorite(Long userId, Long productId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite fav = new Favorite(null, userId, productId, LocalDateTime.now());
            favoriteRepository.save(fav);
            return true;
        }
    }

    public boolean isFavorited(Long userId, Long productId) {
        return favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    public Long getFavoriteCount(Long productId) {
        return (long) favoriteRepository.countByProductId(productId);
    }
}
