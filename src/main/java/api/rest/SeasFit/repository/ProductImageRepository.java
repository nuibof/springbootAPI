package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    // Custom query to find images by product ID
    List<ProductImage> findByProductId(Long productId);
}
