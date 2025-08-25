package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductFullRequest;
import api.rest.SeasFit.dto.QuantityDTO;
import api.rest.SeasFit.dto.VariantOnlyDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import api.rest.SeasFit.service.BannerService;
import api.rest.SeasFit.service.ProductService;
import api.rest.SeasFit.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController {
    private final ProductService productService;
    @Autowired
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
    @Autowired
    private OrderItemRepository orderItemRepository;


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
    @Transactional
    public ResponseEntity<?> createFullProduct(@RequestBody ProductFullRequest request) {

        // ---- Validate tối thiểu ----
        if (request.getCategoryId() == null) {
            return ResponseEntity.badRequest().body("categoryId is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            return ResponseEntity.badRequest().body("name is required");
        }

        // ---- Product ----
        Product product = new Product();
        // set default gender nếu null/rỗng, KHÔNG ghi đè lại sau đó
        Integer gender = request.getGender();
        product.setGender(gender != null ? gender : 0);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");

        Category category = categoryRepository.findById(Long.valueOf(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        product = productRepository.save(product);

        // ---- Colors (an toàn null) ----
        List<ProductFullRequest.ColorRequest> colors = request.getColors() != null
                ? request.getColors() : Collections.emptyList();

        for (ProductFullRequest.ColorRequest colorReq : colors) {
            // Chuẩn hoá input
            String colorName = StringUtils.trimWhitespace(colorReq.getName());
            String hex       = StringUtils.trimWhitespace(colorReq.getHex());
            String imageUrl  = StringUtils.trimWhitespace(colorReq.getImage());

            // Bỏ qua color thiếu dữ liệu cơ bản
            if (!StringUtils.hasText(colorName) && !StringUtils.hasText(hex)) {
                continue;
            }

            // Color
            Optional<Color> existingColor = colorRepository.findByNameAndHexCode(colorName, hex);
            Color color = existingColor.orElseGet(() -> {
                Color c = new Color();
                c.setName(colorName);
                c.setHexCode(hex);
                return colorRepository.save(c);
            });

            // Image (nếu có)
            if (StringUtils.hasText(imageUrl)) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setColor(color);
                image.setImageUrl(imageUrl);
                image.setCreatedAt(LocalDateTime.now());
                productImageRepository.save(image);
            }

            // Giá mặc định = 0 nếu null/âm
            BigDecimal price = colorReq.getPrice() != null
                    ? colorReq.getPrice()
                    : BigDecimal.ZERO;
            if (price.signum() < 0) price = BigDecimal.ZERO;

            // Sizes (lọc null/rỗng, trim, unique)
            List<String> sizes = colorReq.getSizes() != null ? colorReq.getSizes() : Collections.emptyList();
            LinkedHashSet<String> cleanedSizes = sizes.stream()
                    .map(s -> StringUtils.trimWhitespace(s))
                    .filter(StringUtils::hasText)   // <--- chặn NULL/rỗng ở đây
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Nếu không có size hợp lệ thì bỏ qua tạo variant
            if (cleanedSizes.isEmpty()) continue;

            for (String sizeStr : cleanedSizes) {
                // Size entity
                Optional<Size> existingSize = sizeRepository.findByLabel(sizeStr);
                Size size = existingSize.orElseGet(() -> {
                    Size newSize = new Size();
                    newSize.setLabel(sizeStr);
                    return sizeRepository.save(newSize);
                });

                // Variant
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setColor(color);
                variant.setSize(size);
                variant.setQuantity(0);
                variant.setPrice(price);
                variant.setCreatedAt(LocalDateTime.now());
                productVariantRepository.save(variant);
            }
        }

        return ResponseEntity.ok("Product created successfully");
    }


    @PutMapping("/products/active/{id}")
    public ResponseEntity<?> toggleProductActive(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            System.out.println("Current status: " + product.getStatus());

            product.setStatus("ACTIVE".equalsIgnoreCase(product.getStatus()) ? "INACTIVE" : "ACTIVE");

            productRepository.save(product);

            return ResponseEntity.ok(product.getStatus()); // Trả luôn trạng thái mới
        } catch (Exception e) {
            e.printStackTrace(); // để log ra lỗi cụ thể trong console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating product status");
        }

    }
    @GetMapping("/banner")
    public ResponseEntity<Banner> getBanner() {
        try {
            Banner banner = bannerService.getBanner();
            return ResponseEntity.ok(banner);
        } catch (Exception e) {
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
            existingBanner.setButtonUrl(updatedBanner.getButtonUrl());

            existingBanner.setSubImageUrl1(updatedBanner.getSubImageUrl1());
            existingBanner.setSubImageUrl2(updatedBanner.getSubImageUrl2());
            existingBanner.setSubImageUrl3(updatedBanner.getSubImageUrl3());

            existingBanner.setSubText1(updatedBanner.getSubText1());
            existingBanner.setSubText2(updatedBanner.getSubText2());
            existingBanner.setSubText3(updatedBanner.getSubText3());

            existingBanner.setSubButtonText1(updatedBanner.getSubButtonText1());
            existingBanner.setSubButtonText2(updatedBanner.getSubButtonText2());
            existingBanner.setSubButtonText3(updatedBanner.getSubButtonText3());

            existingBanner.setSubButtonUrl1(updatedBanner.getSubButtonUrl1());
            existingBanner.setSubButtonUrl2(updatedBanner.getSubButtonUrl2());
            existingBanner.setSubButtonUrl3(updatedBanner.getSubButtonUrl3());

            System.out.println(existingBanner);
            bannerRepository.save(existingBanner);

            return ResponseEntity.ok("Banner updated successfully");
        } catch (Exception e) {
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
        dto.setCategoryId(product.getCategory().getId());

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

        // ===== 1) Update main fields =====
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());
        product.setGender(request.getGender());

        Category category = categoryRepository.findById(Long.valueOf(request.getCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);
        productRepository.save(product);

        // ===== 2) Load hiện trạng DB =====
        List<ProductVariant> existingVariants = productVariantRepository.findByProductId(product.getId());
        Map<String, ProductVariant> existingVariantKeyMap = new HashMap<>();
        for (ProductVariant v : existingVariants) {
            String key = v.getColor().getId() + "_" + v.getSize().getId();
            existingVariantKeyMap.put(key, v);
        }

        List<ProductImage> existingImages = productImageRepository.findByProductId(product.getId());
        // ảnh map theo colorId (mỗi màu 1 ảnh – theo payload)
        Map<Long, ProductImage> imageByColor = new HashMap<>();
        for (ProductImage img : existingImages) {
            if (img.getColor() != null) {
                imageByColor.put(Long.valueOf(img.getColor().getId()), img);
            }
        }

        // ===== 3) Build set các key variant từ request =====
        Set<String> incomingKeys = new HashSet<>();

        // ===== 4) Duyệt colors trong request: upsert ảnh + upsert variants =====
        for (ProductFullRequest.ColorRequest colorReq : request.getColors()) {
            // upsert Color
            Color color = colorRepository.findByNameAndHexCode(colorReq.getName(), colorReq.getHex())
                    .orElseGet(() -> colorRepository.save(new Color(colorReq.getName(), colorReq.getHex())));

            // upsert Image theo color
            ProductImage img = imageByColor.get(color.getId());
            if (img == null) {
                img = new ProductImage();
                img.setProduct(product);
                img.setColor(color);
            }
            img.setImageUrl(colorReq.getImage());
            img.setCreatedAt(java.time.LocalDateTime.now());
            productImageRepository.save(img);

            // upsert variants theo sizes
            for (String sizeStr : colorReq.getSizes()) {
                Size size = sizeRepository.findByLabel(sizeStr)
                        .orElseGet(() -> sizeRepository.save(new Size(sizeStr)));

                String key = color.getId() + "_" + size.getId();
                incomingKeys.add(key);

                ProductVariant variant = existingVariantKeyMap.get(key);
                if (variant == null) {
                    // CREATE
                    variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setColor(color);
                    variant.setSize(size);
                    variant.setQuantity(0);
                }
                // UPDATE chung
                variant.setPrice(colorReq.getPrice());
                variant.setActive(true); // mở bán
                productVariantRepository.save(variant);
            }
        }

        // ===== 5) Xử lý variants cũ KHÔNG còn trong request =====
        for (ProductVariant old : existingVariants) {
            String key = old.getColor().getId() + "_" + old.getSize().getId();
            if (!incomingKeys.contains(key)) {
                boolean referenced = orderItemRepository.existsByProductVariant_Id(old.getId());
                if (referenced) {
                    // đã từng xuất hiện trong order_item -> không xóa, chỉ disable
                    if (old.isActive()) {
                        old.setActive(false);
                        productVariantRepository.save(old);
                    }
                } else {
                    // chưa dùng -> có thể xóa (hoặc soft-delete tùy ông)
                    productVariantRepository.delete(old);
                }
            }
        }

        return ResponseEntity.ok("Product updated successfully");
    }


}
