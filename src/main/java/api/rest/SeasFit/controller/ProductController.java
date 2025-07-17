package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.entity.Product;
import org.springframework.web.bind.annotation.GetMapping;
import api.rest.SeasFit.service.ProductService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/products/{id}")
    public ProductDetailDTO getProductById(@PathVariable Long id) {
        return productService.getProductDetail(id);
    }
}
