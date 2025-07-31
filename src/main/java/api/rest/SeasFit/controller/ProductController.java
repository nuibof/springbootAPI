package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.dto.ProductListDTO;
import api.rest.SeasFit.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import api.rest.SeasFit.service.ProductService;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
     public List<Product> getAllProducts() {
         return productService.findAll();
     }

    @GetMapping("/products/gender/{gender}")
    public List<Product> getProductsByGender(@PathVariable String gender) {
        return productService.findByGender(gender);
    }
    @GetMapping("/products/{id}")
    public ProductDetailDTO getProductById(@PathVariable Long id) {
        return productService.getProductDetail(id);
    }

    @GetMapping("/products/page")
    public Page<ProductListDTO> getProductsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer colorId,
            @RequestParam(required = false) Integer sizeId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort
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


}
