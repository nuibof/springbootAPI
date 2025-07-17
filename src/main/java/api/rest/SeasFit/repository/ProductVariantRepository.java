package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    // Custom query to find variants by product ID
    List<ProductVariant> findByProductId(Long productId);
    // getcolor
    List<ProductVariant> findByColorId(Long colorId);

}
