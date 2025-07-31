package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByColorId(Integer colorId);

    Optional<ProductVariant> findByProduct_IdAndColor_IdAndSize_Id(Long productId, Integer colorId, Integer sizeId);

    Optional<ProductVariant> findByProductIdAndColorIdAndSizeId(Long product_id, Long color_id, Long size_id);

    void deleteAllByProduct(Product product);

    void deleteByProductId(Long productId);


    boolean existsByProductIdAndColorIdAndSizeId(Long productId, Integer colorId, Integer sizeId);


}
