package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Password;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordRepository extends JpaRepository<Password, Long> {
    Optional<Password> findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(String email, LocalDateTime now);
}
