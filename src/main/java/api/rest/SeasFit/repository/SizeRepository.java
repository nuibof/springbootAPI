package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Long> {
    Optional<Size> findByLabel(String label);

    boolean existsByLabel(String label);
}
