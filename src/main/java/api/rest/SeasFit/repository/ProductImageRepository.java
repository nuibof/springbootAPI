package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdAndColorId(Long productId, Long colorId);

    Optional<ProductImage> findFirstByProduct_IdAndColor_IdOrderByCreatedAtAsc(Long productId, Long colorId);
    Optional<ProductImage> findFirstByProduct_IdOrderByCreatedAtAsc(Long productId);
}
