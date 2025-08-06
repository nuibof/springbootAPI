package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    boolean existsByUserIdAndIsDefaultTrue(Long userId);

    Optional<Address> findFirstByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Address> findByIdAndUserId(Long addressId, Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);


}
