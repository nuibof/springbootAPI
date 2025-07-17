package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductFullRequest;
import api.rest.SeasFit.dto.ProductInputDTO;
import api.rest.SeasFit.dto.QuantityDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import api.rest.SeasFit.service.CategoryService;
import api.rest.SeasFit.service.ProductService;
import api.rest.SeasFit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/data")
public class DataController {
    private final ProductService productService;
    private final UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ColorRepository colorRepository;
    @Autowired
    private SizeRepository sizeRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;


    public DataController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }
     @GetMapping("/products")
     public ResponseEntity<List<Product>> getAllProducts() {
         List<Product> products = productService.findAll();
         return ResponseEntity.ok(products);
     }

     @PostMapping("/products/quantity")
     public ResponseEntity<?> changeQuantity(@RequestBody QuantityDTO quantity) {
         // 1. Tìm sản phẩm theo ID
         ProductVariant productVariant = productVariantRepository.findById((long) quantity.getId())
                 .orElseThrow(() -> new RuntimeException("Product not found"));

         // 2. Cập nhật số lượng
         productVariant.setQuantity(quantity.getQuantity());

         // 3. Lưu lại sản phẩm
         productVariantRepository.save(productVariant);

         return ResponseEntity.ok("Product quantity updated successfully");
     }

    @PostMapping("/products/full")
    public ResponseEntity<?> createFullProduct(@RequestBody ProductFullRequest request) {
        // 1. Tạo đối tượng Product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImage(request.getImageUrl());
        product.setStatus(request.getStatus());

        // 2. Gắn category
        Category category = categoryRepository.findById(Long.valueOf(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        // 3. Lưu product trước để có ID
        product = productRepository.save(product);

        // 4. Duyệt từng màu
        for (ProductFullRequest.ColorRequest colorReq : request.getColors()) {
            // 4.1 Tạo Color (tránh trùng tên và mã màu)
            Optional<Color> existingColor = colorRepository.findByNameAndHexCode(colorReq.getName(), colorReq.getHex());

            Color color = existingColor.orElseGet(() -> {
                Color newColor = new Color();
                newColor.setName(colorReq.getName());
                newColor.setHexCode(colorReq.getHex());
                return colorRepository.save(newColor);
            });

            // 4.2 Lưu product_image cho từng màu
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setColor(color);
            image.setImageUrl(colorReq.getImage());
            image.setCreatedAt(LocalDateTime.now()); // nếu có trường created_at
            productImageRepository.save(image);

            // 4.3 Lưu size (tránh trùng)
            for (String sizeStr : colorReq.getSizes()) {
                Optional<Size> existingSize = sizeRepository.findByLabel(sizeStr);
                Size size = existingSize.orElseGet(() -> {
                    Size newSize = new Size();
                    newSize.setLabel(sizeStr);
                    return sizeRepository.save(newSize);
                });

                // Tạo và lưu ProductVariant
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setColor(color);
                variant.setSize(size);
                variant.setQuantity(0); // hoặc request.getQuantity()
                variant.setCreatedAt(LocalDateTime.now());
                productVariantRepository.save(variant);
            }


        }

        return ResponseEntity.ok("Product created successfully");
    }





}
