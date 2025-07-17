package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Category;
import api.rest.SeasFit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category save(Category entity) {
        return categoryRepository.save(entity);
    }

    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }
}
