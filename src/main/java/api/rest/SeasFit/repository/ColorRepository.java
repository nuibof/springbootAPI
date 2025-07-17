package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByNameAndHexCode(String name, String hex);
}
