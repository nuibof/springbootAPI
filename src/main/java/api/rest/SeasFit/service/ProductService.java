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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<ProductCardDTO> findByGender(String gender) {
        Integer code = toGenderCode(gender); // 1=male, 2=female...
        var products = productRepository.findByGender(code);
        return products.stream()
                .map(p -> new ProductCardDTO(p.getId(), p.getName(), p.getImageUrl()))
                .toList();
    }

    private int toGenderCode(String g) {
        if (g == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender null k hợp lệ");
        }
        switch (g.trim().toLowerCase()) {
            case "male":
            case "m":
            case "nam":
                return 1;
            case "female":
            case "f":
            case "nu":
            case "n":
                return 2;
            case "unisex":
            case "uni":
            case "all":
                return 0;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender phải là male|female|unisex");
        }
    }


    @Transactional
    public Product save(Product entity) {
        return productRepository.save(entity);
    }

    @Transactional
    public void deleteById(Long productId) {
        // 1) Xoá các bảng không ràng buộc lịch sử
        productRepository.softDeleteById(productId);

//        reviewRepository.deleteByProductId(productId);
//        favoriteRepository.deleteByProductId(productId);
            cartItemRepository.deleteByVariant_Product_Id(productId);
//        inventoryRepository.deleteByProductId(productId);
//        productImageRepository.deleteByProductId(productId); // nếu ảnh có FK mềm thì chuyển sang soft-delete
//
//        // 2) Kiểm tra có đơn hàng tham chiếu variant không
//        boolean hasOrders = orderItemRepository.existsByProductVariant_Product_Id(productId);
//
//        if (hasOrders) {
//            // 2a) Không được xoá cứng → soft-delete variants + product
//            productVariantRepository.softDeleteByProductId(productId); // update deleted=1, active=0
//                         // update deleted=1, active=0
//        } else {
//            // 2b) Không có đơn nào → cho phép xoá cứng
//            productVariantRepository.deleteByProductId(productId);
//            productRepository.deleteById(productId);
//        }
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
        // 1) Lấy data (join theo color/size nếu có); KHÔNG lọc min/max ở DB
        Page<Product> products = productRepository.findAll((root, query, cb) -> {

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("variants", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // join variant để lọc color/size (nếu cần)
            if (colorId != null || sizeId != null) {
                Join<Object, Object> vj = root.join("variants", JoinType.INNER);
                if (colorId != null) {
                    predicates.add(cb.equal(vj.get("color").get("id"), colorId));
                }
                if (sizeId != null) {
                    predicates.add(cb.equal(vj.get("size").get("id"), sizeId));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, Pageable.unpaged());

        LocalDateTime now = LocalDateTime.now();

        // 2) Map sang DTO có finalPrice + onSale
        List<ProductListDTO> dtoList = products.getContent().stream()
                .map(product -> {
                    // Tập variant hợp lệ của sản phẩm
                    List<ProductVariant> vars = product.getVariants();

                    // min giá gốc theo sản phẩm
                    BigDecimal minOriginal = vars.stream()
                            .map(ProductVariant::getPrice)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);

                    // min finalPrice theo sản phẩm
                    BigDecimal minFinal = vars.stream()
                            .map(v -> finalPrice(
                                    v.getPrice(),
                                    v.getSaleAmount(),  // <-- field mới trong entity
                                    v.getSaleFrom(),
                                    v.getSaleTo(),
                                    now))
                            .min(Comparator.naturalOrder())
                            .orElse(minOriginal);

                    boolean onSale = minFinal.compareTo(minOriginal) < 0;

                    // Color previews (đổi nghĩa: price = min FINAL price theo màu)
                    Map<Integer, Color> colorMap = vars.stream()
                            .map(ProductVariant::getColor)
                            .collect(Collectors.toMap(Color::getId, c -> c, (c1, c2) -> c1));

                    List<ColorDTO> colorPreviews = colorMap.values().stream()
                            .map(color -> {
                                List<ProductVariant> byColor = vars.stream()
                                        .filter(v -> v.getColor().getId().equals(color.getId()))
                                        .toList();

                                // min FINAL price theo màu
                                BigDecimal minFinalColor = byColor.stream()
                                        .map(v -> finalPrice(
                                                v.getPrice(),
                                                v.getSaleAmount(),
                                                v.getSaleFrom(),
                                                v.getSaleTo(),
                                                now))
                                        .min(Comparator.naturalOrder())
                                        .orElse(BigDecimal.ZERO);

                                // sizes theo màu (giữ nguyên)
                                List<SizeDTO> sizesForColor = byColor.stream()
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

                                // GIỮ constructor cũ: ... imagePreview, price, sizes
                                // => ở đây "price" = minFinalColor để FE hiện đúng giá đang sale
                                return new ColorDTO(
                                        color.getId(),
                                        color.getName(),
                                        color.getHexCode(),
                                        imagePreview,
                                        minFinalColor,
                                        sizesForColor
                                );
                            })
                            .toList();

                    // sizes toàn sp (giữ nguyên)
                    List<SizeDTO> sizes = vars.stream()
                            .map(ProductVariant::getSize)
                            .filter(Objects::nonNull)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(Size::getId, sz -> sz, (s1, s2) -> s1),
                                    m -> m.values().stream()
                                            .map(sz -> new SizeDTO(sz.getId(), sz.getLabel()))
                                            .toList()
                            ));

                    // ✅ ProductListDTO mở rộng: thêm finalPrice + onSale
                    return new ProductListDTO(
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            minOriginal,          // giá gốc nhỏ nhất
                            minFinal,             // giá sau sale nhỏ nhất
                            onSale,               // có đang sale không
                            colorPreviews,
                            sizes
                    );
                    // cần có setter trong DTO
                })
                .toList();

        // 3) Lọc khoảng giá theo FINAL price (fallback original nếu null)
        if (minPrice != null) {
            dtoList = dtoList.stream()
                    .filter(p -> {
                        BigDecimal base = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();
                        return base.compareTo(minPrice) >= 0;
                    })
                    .toList();
        }
        if (maxPrice != null) {
            dtoList = dtoList.stream()
                    .filter(p -> {
                        BigDecimal base = p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice();
                        return base.compareTo(maxPrice) <= 0;
                    })
                    .toList();
        }

        // 4) Sort theo FINAL price khi sort=dummy (giữ cú pháp cũ “price,asc|desc” ở controller)
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            if ("dummy".equals(order.getProperty())) {
                Comparator<ProductListDTO> cmp = Comparator.comparing(p ->
                        p.getFinalPrice() != null ? p.getFinalPrice() : p.getPrice()
                );
                if (order.getDirection().isDescending()) cmp = cmp.reversed();
                dtoList = dtoList.stream().sorted(cmp).toList();
            }
        }

        // 5) Tự phân trang
        int start = page * size;
        int end = Math.min(start + size, dtoList.size());
        List<ProductListDTO> pagedList = dtoList.subList(Math.min(start, end), end);

        return new PageImpl<>(pagedList, PageRequest.of(page, size, sort), dtoList.size());
    }




    public ProductDetailDTO getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow();

        List<ProductVariant> variants = productVariantRepository.findByProductId(id);
        List<ProductImage> images   = productImageRepository.findByProductId(id);
        List<Review> reviews        = reviewRepository.findByProductId(id);

        LocalDateTime now = LocalDateTime.now();

        Map<Integer, ProductDetailDTO.ColorDTO> colorMap = new LinkedHashMap<>();

        // ===== gom biến thể theo màu, map size + tính giá =====
        for (ProductVariant variant : variants) {
            if (variant.getColor() == null || variant.getSize() == null) continue;

            int colorId = variant.getColor().getId();
            ProductDetailDTO.ColorDTO colorDTO = colorMap.computeIfAbsent(colorId, k -> {
                ProductDetailDTO.ColorDTO dto = new ProductDetailDTO.ColorDTO();
                dto.setId(colorId);
                dto.setName(variant.getColor().getName());
                dto.setHex(variant.getColor().getHexCode());
                dto.setSizes(new ArrayList<>());

                // ảnh preview theo màu (nếu có)
                String preview = images.stream()
                        .filter(img -> img.getColor() != null && img.getColor().getId() == colorId)
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null);
                dto.setImage(preview);

                // init các field tổng hợp
                dto.setMinPrice(null);
                dto.setMinFinalPrice(null);
                dto.setOnSale(false);
                return dto;
            });

            // tính onSale cho biến thể
            BigDecimal price = default0(variant.getPrice());
            BigDecimal saleAmount = default0(variant.getSaleAmount());
            LocalDateTime from = variant.getSaleFrom();
            LocalDateTime to   = variant.getSaleTo();

            boolean sizeOnSale =
                    saleAmount.compareTo(BigDecimal.ZERO) > 0 &&
                            (from == null || !now.isBefore(from)) &&
                            (to   == null || !now.isAfter(to));

            BigDecimal effectiveSale = sizeOnSale ? saleAmount : BigDecimal.ZERO;
            BigDecimal finalPrice = price.subtract(effectiveSale);
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) finalPrice = BigDecimal.ZERO;

            // add size
            ProductDetailDTO.SizeDTO sizeDTO = new ProductDetailDTO.SizeDTO();
            sizeDTO.setId(variant.getSize().getId());
            sizeDTO.setLabel(variant.getSize().getLabel());
            sizeDTO.setQuantity(defaultInt(variant.getQuantity()));

            sizeDTO.setPrice(price);
            sizeDTO.setSaleAmount(saleAmount);
            sizeDTO.setFinalPrice(finalPrice);
            sizeDTO.setSaleFrom(from);
            sizeDTO.setSaleTo(to);
            sizeDTO.setOnSale(sizeOnSale);

            colorDTO.getSizes().add(sizeDTO);

            // cập nhật tổng hợp theo màu
            colorDTO.setMinPrice(minBD(colorDTO.getMinPrice(), price));
            colorDTO.setMinFinalPrice(minBD(colorDTO.getMinFinalPrice(), finalPrice));
            if (sizeOnSale) colorDTO.setOnSale(true);
        }

        // ===== Reviews =====
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

        // ===== Tổng hợp cấp sản phẩm =====
        BigDecimal minOriginal = null;
        BigDecimal minFinal    = null;
        boolean anyOnSale      = false;

        for (ProductDetailDTO.ColorDTO c : colorMap.values()) {
            minOriginal = minBD(minOriginal, default0(c.getMinPrice()));
            minFinal    = minBD(minFinal,    default0(c.getMinFinalPrice()));
            if (c.isOnSale()) anyOnSale = true;
        }

        // fallback khi không có biến thể
        if (minOriginal == null) minOriginal = BigDecimal.ZERO;
        if (minFinal == null)    minFinal    = minOriginal;

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());

        // giữ trường cũ là "giá gốc rẻ nhất"
        dto.setPrice(minOriginal);
        // bổ sung
        dto.setFinalPrice(minFinal);
        dto.setOnSale(anyOnSale);

        dto.setColors(new ArrayList<>(colorMap.values()));
        dto.setFavorites(favoriteCount);
        dto.setRating(rating);
        dto.setStatus(product.getStatus());
        dto.setReviews(reviewDTOs);

        return dto;
    }

    private static BigDecimal default0(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }
    private static int defaultInt(Integer x) {
        return x == null ? 0 : x;
    }
    private static BigDecimal minBD(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) <= 0 ? a : b;
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

    public List<ProductSuggestDTO> suggestByName(String q, int limit) {
        if (q == null || q.isBlank()) return List.of();

        int top = Math.min(Math.max(limit, 1), 20);
        var page = PageRequest.of(0, top);

        // repo sẽ order theo updatedAt desc (nếu có) hoặc id desc
        List<Product> list = productRepository.searchTop(q.trim(), page);

        return list.stream()
                .map(p -> new ProductSuggestDTO(
                        p.getId(),
                        p.getName(),
                        minVariantPrice(p),
                        pickThumb(p)
                ))
                .toList();
    }

    private BigDecimal minVariantPrice(Product p) {
        // an toàn nếu chưa load variants
        return p.getVariants() == null ? BigDecimal.ZERO :
                p.getVariants().stream()
                        .map(ProductVariant::getPrice)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);
    }

    private String pickThumb(Product p) {
        // ưu tiên field ảnh chính nếu có, không thì lấy ảnh đầu tiên
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            return p.getImageUrl();
        }
        return productImageRepository.findByProductId(p.getId()).stream()
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    private boolean isSaleActive(LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        if (from != null && now.isBefore(from)) return false;
        if (to != null && now.isAfter(to)) return false;
        return true;
    }

    private BigDecimal finalPrice(BigDecimal price,
                                  BigDecimal saleAmount,
                                  LocalDateTime from,
                                  LocalDateTime to,
                                  LocalDateTime now) {
        if (price == null) return BigDecimal.ZERO;
        if (saleAmount == null || saleAmount.signum() <= 0) return price;
        if (!isSaleActive(from, to, now)) return price;

        BigDecimal fp = price.subtract(saleAmount);
        return fp.signum() < 0 ? BigDecimal.ZERO : fp;
    }

    @Transactional
    public void updateSaleForProduct(Long productId, UpdateSaleRequest req) {
        if (req.isClear()) {
            productVariantRepository.clearSale(productId);
            return;
        }
        BigDecimal amt = req.getSaleAmount() == null ? BigDecimal.ZERO : req.getSaleAmount();
        if (amt.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("saleAmount must be >= 0");
        }
        if (req.getSaleFrom() != null && req.getSaleTo() != null && req.getSaleFrom().isAfter(req.getSaleTo())) {
            throw new IllegalArgumentException("saleFrom must be before saleTo");
        }
        productVariantRepository.bulkUpdateSale(productId, amt, req.getSaleFrom(), req.getSaleTo());
    }

}
