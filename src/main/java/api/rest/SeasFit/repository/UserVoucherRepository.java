// api/rest/SeasFit/repository/UserVoucherRepository.java
package api.rest.SeasFit.repository;

import api.rest.SeasFit.dto.UserVoucherDTO;
import api.rest.SeasFit.entity.UserVoucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    boolean existsByUser_IdAndVoucher_Code(Long userId, String code);

    List<UserVoucher> findByUser_Id(Long userId);

    Optional<UserVoucher> findByUser_IdAndVoucher_Code(Long userId, String code);

    @Query("""
       select uv from UserVoucher uv
       join fetch uv.voucher v
       where uv.user.id = :userId
    """)
    List<UserVoucher> findByUserIdFetchVoucher(@Param("userId") Long userId);

    @Query("""
  select new api.rest.SeasFit.dto.UserVoucherDTO(
      uv.id,
      v.code,
      v.description,
      v.discountType,
      v.discountValue,
      v.minOrderAmount,
      v.startDate,
      v.endDate,
      v.isActive,
      v.maxDiscountValue
  )
  from UserVoucher uv
  join uv.voucher v
  where uv.user.id = :userId
  order by v.endDate asc NULLS LAST, uv.id desc
""")
    List<UserVoucherDTO> findAllForUserDTO(@Param("userId") Long userId);


}
