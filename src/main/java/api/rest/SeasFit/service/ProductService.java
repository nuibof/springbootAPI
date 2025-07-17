package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.dto.ProductInputDTO;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;


    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product save(Product entity) {
        return productRepository.save(entity);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    public ProductDetailDTO getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow();

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        List<ProductImage> images = productImageRepository.findByProductId(id);
        List<Review> reviews = reviewRepository.findByProductId(id);

        // Gom nhóm theo ColorId → ColorDTO
        Map<Integer, ProductDetailDTO.ColorDTO> colorMap = new LinkedHashMap<>();

        for (ProductVariant variant : variants) {
            int colorId = variant.getColor().getId();
            ProductDetailDTO.ColorDTO colorDTO = colorMap.computeIfAbsent(colorId, k -> {
                ProductDetailDTO.ColorDTO dto = new ProductDetailDTO.ColorDTO();
                dto.setId(colorId);
                dto.setName(variant.getColor().getName());
                dto.setHex(variant.getColor().getHexCode());
                dto.setSizes(new ArrayList<>());
                dto.setImage(images.stream()
                        .filter(img -> img.getColor().getId() == colorId)
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElse(null));
                return dto;
            });

            // Thêm size với quantity
            ProductDetailDTO.SizeDTO sizeDTO = new ProductDetailDTO.SizeDTO();
            sizeDTO.setId(variant.getSize().getId());
            sizeDTO.setLabel(variant.getSize().getLabel());
            sizeDTO.setQuantity(variant.getQuantity());
            colorDTO.getSizes().add(sizeDTO);
        }

        // Map reviews
        List<ProductDetailDTO.ReviewDTO> reviewDTOs = reviews.stream().map(review -> {
            ProductDetailDTO.ReviewDTO dto = new ProductDetailDTO.ReviewDTO();
            dto.setId(review.getId());
            dto.setContent(review.getComment());
            dto.setRating(review.getRating());
            dto.setCreatedAt(review.getCreatedAt());

            User user = userRepository.findById(review.getUserId()).orElse(null);
            dto.setUserName(user != null ? user.getFullName() : "Ẩn danh");
            return dto;
        }).collect(Collectors.toList());

        int favoriteCount = favoriteRepository.countByProductId(id);
        Double rating = reviewRepository.avgRatingByProductId(id);

        // Kết quả cuối
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setColors(new ArrayList<>(colorMap.values()));
        dto.setFavorites(favoriteCount);
        dto.setRating(rating);
        dto.setReviews(reviewDTOs);

        return dto;
    }


    public int getTotalProducts() {
        return (int) productRepository.count();
    }
    public int getLowStockProducts() {
        return productRepository.countProductsLowStock();
    }

    @Transactional
    public void createProductWithVariants(ProductInputDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStatus(dto.getStatus());

        Category category = categoryRepository.findById((long) dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        product = productRepository.save(product);

        for (ProductInputDTO.ColorVariantDTO colorDTO : dto.getColors()) {
            Color color = colorRepository.findById((long) colorDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Color not found"));

            // Save ProductImage
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setColor(color);
            image.setImageUrl(colorDTO.getImage());
            productImageRepository.save(image);

            for (String sizeLabel : colorDTO.getSizes()) {
                Size size = (Size) sizeRepository.findByLabel(sizeLabel)
                        .orElseThrow(() -> new RuntimeException("Size not found"));

                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setColor(color);
                variant.setSize(size);
                variant.setQuantity(100); // Default test
                productVariantRepository.save(variant);
            }
        }
    }



}
