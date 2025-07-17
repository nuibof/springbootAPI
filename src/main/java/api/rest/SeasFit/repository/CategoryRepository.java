package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
