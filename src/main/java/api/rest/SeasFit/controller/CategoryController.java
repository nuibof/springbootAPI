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

}
