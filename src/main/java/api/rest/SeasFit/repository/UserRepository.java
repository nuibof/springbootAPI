package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.User;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByUserName(String userName);
    Optional<User> findByEmail(String email);
    User findByPhone(String phone);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findLatestUsers(Pageable pageable);


    List<User> findTop5ByOrderByCreatedAtDesc();




}
