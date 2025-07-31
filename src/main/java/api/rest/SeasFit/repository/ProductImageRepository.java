package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdAndColorId(Long productId, Long colorId);

    void deleteAllByProduct(Product product);

    void deleteByProductId(Long productId);
}
