package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    boolean existsByCode(String code);
    Optional<Voucher> findByCode(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voucher v set v.quantity = v.quantity - 1 " +
            "where v.code = :code and v.isActive = true and v.quantity > 0")
    int decrementQty(@Param("code") String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voucher v set v.quantity = v.quantity + 1 where v.code = :code")
    int incrementQty(@Param("code") String code);

}
