package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.*;
import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.service.ProductAiService;
import api.rest.SeasFit.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductAiService productAiService;
    public ProductController(ProductService productService, ProductAiService productAiService) {
        this.productService = productService;
        this.productAiService = productAiService;
    }

    // GET /api/products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }

    // GET /api/products/gender/{gender}
    @GetMapping("/gender/{gender}")
    public List<ProductCardDTO> getProductsByGender(@PathVariable String gender) {
        return productService.findByGender(gender);
    }

    @GetMapping("/bestsellers")
    public List<Map<String, Object>> bestsellers(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false, defaultValue = "8") Integer limit
    ) {
        var list = productService.getBestsellers(days, limit);

        // ĐỪNG Map.of → dùng LinkedHashMap để type = Map<String,Object> sạch sẽ
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (var b : list) {
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("totalSold", b.getTotalSold());
            m.put("url", "/product/" + b.getId());
            result.add(m);
        }
        return result;
    }


    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ProductDetailDTO getProductById(@PathVariable Long id) {
        return productService.getProductDetail(id);
    }

    // GET /api/products/page
    // sort: "price,asc" | "price,desc"  => map sang Sort.by("dummy") để service sort thủ công theo price của DTO
    @GetMapping("/page")
    public Page<ProductListDTO> getProductsPage(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer colorId, @RequestParam(required = false) Integer sizeId, @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) String sort
    ) {
        Sort sortObject = Sort.unsorted();
        if ("price,asc".equalsIgnoreCase(sort)) {
            sortObject = Sort.by(Sort.Order.asc("dummy"));
        } else if ("price,desc".equalsIgnoreCase(sort)) {
            sortObject = Sort.by(Sort.Order.desc("dummy"));
        }

        return productService.findAllWithFilters(
                categoryId, colorId, sizeId, minPrice, maxPrice, page, size, sortObject
        );
    }

    // GET /api/products/suggest?q=...&limit=8
    @GetMapping("/suggest")
    public ResponseEntity<List<ProductSuggestDTO>> suggest(
            @RequestParam("q") String q, @RequestParam(value = "limit", defaultValue = "8") int limit
    ) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        int top = Math.min(Math.max(limit, 1), 20);
        return ResponseEntity.ok(productService.suggestByName(q.trim(), top));
    }

//    @PostMapping("/ai/recommend")
//    public AiSuggestResponse aiRecommend(@RequestBody AiSuggestRequest req) {
//        return productAiService.recommend(req);
//    }
}
