package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.*;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
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
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;





    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findByGender(String gender) {
        return productRepository.findByGender(gender);
    }
    @Transactional
    public Product save(Product entity) {
        return productRepository.save(entity);
    }

    @Transactional
    public void deleteById(Long productId) {
        // Xoá dữ liệu liên quan trước
        reviewRepository.deleteByProductId(productId);
        favoriteRepository.deleteByProductId(productId);
        cartItemRepository.deleteByProductId(productId);
        inventoryRepository.deleteByProductId(productId);
        productImageRepository.deleteByProductId(productId);
        productVariantRepository.deleteByProductId(productId);

        // Sau cùng xoá product
        productRepository.deleteById(productId);
    }


    public Page<ProductListDTO> findAllWithFilters(
            Long categoryId,
            Integer colorId,
            Integer sizeId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            Sort sort
    ) {
        Pageable pageable = PageRequest.of(page, size); // ⚠️ Không dùng sort ở đây vì sort sẽ làm lỗi nếu sort theo "price"


        Page<Product> products = productRepository.findAll((root, query, cb) -> {

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("variants", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (colorId != null || sizeId != null || minPrice != null || maxPrice != null) {
                Join<Object, Object> variantJoin = root.join("variants", JoinType.INNER);

                if (colorId != null) {
                    predicates.add(cb.equal(variantJoin.get("color").get("id"), colorId));
                }
                if (sizeId != null) {
                    predicates.add(cb.equal(variantJoin.get("size").get("id"), sizeId));
                }
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(variantJoin.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(variantJoin.get("price"), maxPrice));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, Pageable.unpaged());

        List<ProductListDTO> dtoList = products.getContent().stream()
                .map(product -> {
                    BigDecimal minVariantPrice = product.getVariants().stream()
                            .map(ProductVariant::getPrice)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);

                    Map<Integer, Color> colorMap = product.getVariants().stream()
                            .map(ProductVariant::getColor)
                            .collect(Collectors.toMap(Color::getId, c -> c, (c1, c2) -> c1));

                    List<ColorDTO> colorPreviews = colorMap.values().stream()
                            .map(color -> {
                                List<ProductVariant> colorVariants = product.getVariants().stream()
                                        .filter(v -> v.getColor().getId().equals(color.getId()))
                                        .toList();

                                BigDecimal minPriceForColor = colorVariants.stream()
                                        .map(ProductVariant::getPrice)
                                        .filter(Objects::nonNull)
                                        .min(Comparator.naturalOrder())
                                        .orElse(BigDecimal.ZERO);

                                List<SizeDTO> sizesForColor = colorVariants.stream()
                                        .map(ProductVariant::getSize)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.collectingAndThen(
                                                Collectors.toMap(Size::getId, s -> s, (s1, s2) -> s1),
                                                m -> m.values().stream()
                                                        .map(sz -> new SizeDTO(sz.getId(), sz.getLabel()))
                                                        .toList()
                                        ));

                                String imagePreview = productImageRepository
                                        .findByProductIdAndColorId(product.getId(), color.getId().longValue())
                                        .stream()
                                        .map(ProductImage::getImageUrl)
                                        .findFirst()
                                        .orElse(null);

                                return new ColorDTO(
                                        color.getId(),
                                        color.getName(),
                                        color.getHexCode(),
                                        imagePreview,
                                        minPriceForColor,
                                        sizesForColor
                                );
                            })
                            .toList();

                    List<SizeDTO> sizes = product.getVariants().stream()
                            .map(ProductVariant::getSize)
                            .filter(Objects::nonNull)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(Size::getId, sz -> sz, (s1, s2) -> s1),
                                    m -> m.values().stream()
                                            .map(sz -> new SizeDTO(sz.getId(), sz.getLabel()))
                                            .toList()
                            ));

                    return new ProductListDTO(
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            minVariantPrice,
                            colorPreviews,
                            sizes
                    );
                })
                .toList();

        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            if ("dummy".equals(order.getProperty())) {
                Comparator<ProductListDTO> comparator = Comparator.comparing(ProductListDTO::getPrice);
                if (order.getDirection().isDescending()) {
                    comparator = comparator.reversed();
                }
                dtoList = dtoList.stream().sorted(comparator).toList();
            }
        }

        int start = page * size;
        int end = Math.min(start + size, dtoList.size());
        List<ProductListDTO> pagedList = dtoList.subList(Math.min(start, end), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size, sort), dtoList.size());
    }



    public ProductDetailDTO getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow();

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        List<ProductImage> images = productImageRepository.findByProductId(id);
        List<Review> reviews = reviewRepository.findByProductId(id);

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

            ProductDetailDTO.SizeDTO sizeDTO = new ProductDetailDTO.SizeDTO();
            sizeDTO.setId(variant.getSize().getId());
            sizeDTO.setLabel(variant.getSize().getLabel());
            sizeDTO.setQuantity(variant.getQuantity());
            colorDTO.getSizes().add(sizeDTO);
        }

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

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        BigDecimal minPrice = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        dto.setPrice(minPrice);

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

    public List<String> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(Product::getName)
                .collect(Collectors.toList());
    }

    private ProductAdminDTO toAdminDTO(Product product) {
        List<ProductVariant> variants = product.getVariants();
        int totalQty = variants.stream().mapToInt(ProductVariant::getQuantity).sum();

        BigDecimal minPrice = variants.stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = variants.stream()
                .map(ProductVariant::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new ProductAdminDTO(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getGender() == 1 ? "Nam" : product.getGender() == 2 ? "Nữ" : "Unisex",
                product.getStatus(),
                product.getCreatedAt(),
                variants.size(),
                totalQty,
                minPrice,
                maxPrice
        );
    }


    public Page<ProductAdminDTO> searchAdminProducts(String keyword, String status, Long categoryId, Integer gender, Pageable pageable) {
        return productRepository.searchAdminProducts(keyword, status, categoryId, gender, pageable)
                .map(this::toAdminDTO);
    }


}
