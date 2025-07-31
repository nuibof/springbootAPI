package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductFullRequest;
import api.rest.SeasFit.dto.ProductInputDTO;
import api.rest.SeasFit.dto.QuantityDTO;
import api.rest.SeasFit.dto.VariantOnlyDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import api.rest.SeasFit.service.BannerService;
import api.rest.SeasFit.service.CategoryService;
import api.rest.SeasFit.service.ProductService;
import api.rest.SeasFit.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Autowired
    private BannerRepository bannerRepository;
    @Autowired
    private BannerService bannerService;


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

         ProductVariant productVariant = productVariantRepository.findById((long) quantity.getId())
                 .orElseThrow(() -> new RuntimeException("Product not found"));

         productVariant.setQuantity(quantity.getQuantity());

         productVariantRepository.save(productVariant);

         return ResponseEntity.ok("Product quantity updated successfully");
     }

    @GetMapping("/products/{productId}/variants")
    public ResponseEntity<?> getVariantsByProduct(@PathVariable Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        List<VariantOnlyDTO> result = variants.stream()
                .map(VariantOnlyDTO::new)
                .toList();
        return ResponseEntity.ok(result);
    }



    @PostMapping("/products/full")
    public ResponseEntity<?> createFullProduct(@RequestBody ProductFullRequest request) {
        Product product = new Product();
        if(request.getGender()==null || request.getGender().equals("")){
            product.setGender(0);
        }
        product.setGender(request.getGender());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());

        Category category = categoryRepository.findById(Long.valueOf(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        product = productRepository.save(product);

        for (ProductFullRequest.ColorRequest colorReq : request.getColors()) {
            Optional<Color> existingColor = colorRepository.findByNameAndHexCode(colorReq.getName(), colorReq.getHex());

            Color color = existingColor.orElseGet(() -> {
                Color newColor = new Color();
                newColor.setName(colorReq.getName());
                newColor.setHexCode(colorReq.getHex());
                return colorRepository.save(newColor);
            });

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setColor(color);
            image.setImageUrl(colorReq.getImage());
            image.setCreatedAt(LocalDateTime.now());
            productImageRepository.save(image);

            for (String sizeStr : colorReq.getSizes()) {
                Optional<Size> existingSize = sizeRepository.findByLabel(sizeStr);
                Size size = existingSize.orElseGet(() -> {
                    Size newSize = new Size();
                    newSize.setLabel(sizeStr);
                    return sizeRepository.save(newSize);
                });

                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setColor(color);
                variant.setSize(size);
                variant.setQuantity(0);
                variant.setPrice(colorReq.getPrice());
                variant.setCreatedAt(LocalDateTime.now());
                productVariantRepository.save(variant);
            }

        }

        return ResponseEntity.ok("Product created successfully");
    }

    @GetMapping("/banner")
    public ResponseEntity<Banner> getBanner() {
        try {
            Banner banner = bannerService.getBanner();
            return ResponseEntity.ok(banner);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/banner")
    public ResponseEntity<?> updateBanner(@RequestBody Banner updatedBanner) {
        try {
            Banner existingBanner = bannerRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Banner not found"));

            existingBanner.setImageUrl(updatedBanner.getImageUrl());
            existingBanner.setBigText(updatedBanner.getBigText());
            existingBanner.setSubText(updatedBanner.getSubText());
            existingBanner.setButtonText(updatedBanner.getButtonText());
            System.out.println(existingBanner);
            bannerRepository.save(existingBanner);

            return ResponseEntity.ok("Banner updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating banner");
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductFullRequest dto = new ProductFullRequest();
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setStatus(product.getStatus());
        dto.setGender(product.getGender());
        dto.setCategoryId(product.getCategory().getId().intValue());

        List<ProductFullRequest.ColorRequest> colors = product.getVariants().stream()
                .collect(Collectors.groupingBy(variant -> variant.getColor().getId()))
                .values().stream().map(variants -> {
                    ProductVariant first = variants.get(0);
                    ProductFullRequest.ColorRequest cr = new ProductFullRequest.ColorRequest();
                    cr.setName(first.getColor().getName());
                    cr.setHex(first.getColor().getHexCode());
                    cr.setPrice(first.getPrice());
                    cr.setSizes(variants.stream()
                            .map(v -> v.getSize().getLabel())
                            .distinct()
                            .toList());
                    // Lấy ảnh từ bảng ảnh
                    String image = productImageRepository
                            .findByProductIdAndColorId(product.getId(), first.getColor().getId().longValue())
                            .stream()
                            .map(ProductImage::getImageUrl)
                            .findFirst().orElse(null);
                    cr.setImage(image);
                    return cr;
                }).toList();

        dto.setColors(colors);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/products/full/{id}")
    @Transactional
    public ResponseEntity<?> updateFullProduct(@PathVariable Long id, @RequestBody ProductFullRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update main fields
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());
        product.setGender(request.getGender());

        Category category = categoryRepository.findById(Long.valueOf(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        productRepository.save(product);

        // Delete old variants and images
        productVariantRepository.deleteAllByProduct(product);
        productImageRepository.deleteAllByProduct(product);

        for (ProductFullRequest.ColorRequest colorReq : request.getColors()) {
            Color color = colorRepository.findByNameAndHexCode(colorReq.getName(), colorReq.getHex())
                    .orElseGet(() -> colorRepository.save(new Color(colorReq.getName(), colorReq.getHex())));

            // Save image per color
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setColor(color);
            image.setImageUrl(colorReq.getImage());
            image.setCreatedAt(LocalDateTime.now());
            productImageRepository.save(image);

            for (String sizeStr : colorReq.getSizes()) {
                Size size = sizeRepository.findByLabel(sizeStr)
                        .orElseGet(() -> sizeRepository.save(new Size(sizeStr)));

                // Avoid inserting duplicate variant
                boolean exists = productVariantRepository.existsByProductIdAndColorIdAndSizeId(product.getId(), color.getId(), size.getId());
                if (!exists) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setColor(color);
                    variant.setSize(size);
                    variant.setQuantity(0);
                    variant.setPrice(colorReq.getPrice());
                    variant.setCreatedAt(LocalDateTime.now());
                    productVariantRepository.save(variant);
                }
            }
        }

        return ResponseEntity.ok("Product updated successfully");
    }



}
