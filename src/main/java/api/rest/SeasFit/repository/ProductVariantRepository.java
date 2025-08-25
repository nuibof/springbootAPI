package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndColorIdAndSizeId(Long productId, Long colorId, Long sizeId);


    // >>> thêm method này để fix compile lỗi <<<
    List<ProductVariant> findByProductIdIn(Collection<Long> productIds);

    // Khóa ghi
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select v from ProductVariant v
        where v.product.id = :productId and v.color.id = :colorId and v.size.id = :sizeId
    """)
    Optional<ProductVariant> findForUpdate(@Param("productId") Long productId,
                                           @Param("colorId") Long colorId,
                                           @Param("sizeId") Long sizeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    // Soft-delete thay cho delete cứng
    @Modifying
    @Query("update ProductVariant v set v.deleted = true, v.version = v.version + 1 where v.id = :id")
    int softDeleteById(@Param("id") Long id);

    @Modifying
    @Query("update ProductVariant v set v.deleted = true, v.active = false where v.product.id = :productId")
    int softDeleteByProductId(@Param("productId") Long productId);

    void deleteByProductId(Long productId);

    // (Tùy chọn) Lấy cả deleted (native)
    @Query(value = "select * from product_variant where product_id = :productId", nativeQuery = true)
    List<ProductVariant> findAllIncludingDeletedByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("""
           update ProductVariant v
              set v.saleAmount = :saleAmount,
                  v.saleFrom   = :saleFrom,
                  v.saleTo     = :saleTo
            where v.product.id = :productId
              and v.deleted = false
           """)
    int bulkUpdateSale(@Param("productId") Long productId,
                       @Param("saleAmount") BigDecimal saleAmount,
                       @Param("saleFrom") LocalDateTime saleFrom,
                       @Param("saleTo") LocalDateTime saleTo);

    @Modifying
    @Query("""
           update ProductVariant v
              set v.saleAmount = 0,
                  v.saleFrom   = null,
                  v.saleTo     = null
            where v.product.id = :productId
              and v.deleted = false
           """)
    int clearSale(@Param("productId") Long productId);
}

