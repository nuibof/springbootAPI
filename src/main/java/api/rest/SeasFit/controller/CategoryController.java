package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.Category;
import api.rest.SeasFit.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    @GetMapping("/all")
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }
    @PostMapping("/add")
    public Category addCategory(Category category) {
        return categoryService.save(category);
    }
    @PutMapping("/{id}")
    public Category updateCategory(@PathVariable Long id, @RequestBody Category updated) {
        Category existing = categoryService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        existing.setName(updated.getName());
        return categoryService.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

}
