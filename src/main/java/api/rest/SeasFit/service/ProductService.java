package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.*;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext
    private EntityManager em;
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
        Page<Product> products = productRepository.findAll((root, query, cb) -> {

            // tránh nhân bản khi join
            query.distinct(true);

            // fetch variants để tránh N+1 (chỉ với query non-count)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("variants", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            // chỉ show ACTIVE (ẩn DELETE/INACTIVE)
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // ✅ lọc theo categoryId
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // join variant để lọc color/size (nếu cần)
            if (colorId != null || sizeId != null) {
                Join<Object, Object> vj = root.join("variants", JoinType.INNER);
                if (colorId != null) {
                    predicates.add(cb.equal(vj.get("color").get("id"), colorId));
                }
                if (sizeId != null) {
                    predicates.add(cb.equal(vj.get("size").get("id"), sizeId));
                }
                // chỉ lấy variant còn hoạt động khi đã buộc join
                predicates.add(cb.isFalse(vj.get("deleted")));
                predicates.add(cb.isTrue(vj.get("active")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, Pageable.unpaged());

        LocalDateTime now = LocalDateTime.now();

        // Map → DTO (lọc variants hợp lệ trước khi tính giá)
        List<ProductListDTO> dtoList = products.getContent().stream()
                .map(p -> {
                    List<ProductVariant> vars = p.getVariants().stream()
                            .filter(v -> v != null && !Boolean.TRUE.equals(v.isDeleted()) && Boolean.TRUE.equals(v.isActive()))
                            .toList();

                    BigDecimal minOriginal = vars.stream()
                            .map(ProductVariant::getPrice)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(BigDecimal.ZERO);

                    BigDecimal minFinal = vars.stream()
                            .map(v -> finalPrice(v.getPrice(), v.getSaleAmount(), v.getSaleFrom(), v.getSaleTo(), now))
                            .min(Comparator.naturalOrder())
                            .orElse(minOriginal);

                    boolean onSale = minFinal.compareTo(minOriginal) < 0;

                    // colors: min FINAL price theo màu + sizes theo màu
                    Map<Integer, Color> colorMap = vars.stream()
                            .map(ProductVariant::getColor)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(Color::getId, c -> c, (a, b) -> a));

                    List<ColorDTO> colorPreviews = colorMap.values().stream()
                            .map(color -> {
                                List<ProductVariant> byColor = vars.stream()
                                        .filter(v -> v.getColor() != null && Objects.equals(v.getColor().getId(), color.getId()))
                                        .toList();

                                BigDecimal minFinalColor = byColor.stream()
                                        .map(v -> finalPrice(v.getPrice(), v.getSaleAmount(), v.getSaleFrom(), v.getSaleTo(), now))
                                        .min(Comparator.naturalOrder())
                                        .orElse(BigDecimal.ZERO);

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
                                        .findByProductIdAndColorId(p.getId(), color.getId().longValue())
                                        .stream()
                                        .map(ProductImage::getImageUrl)
                                        .findFirst()
                                        .orElse(null);

                                // price = minFinalColor để FE hiển thị đúng giá đang sale theo màu
                                return new ColorDTO(color.getId(), color.getName(), color.getHexCode(), imagePreview, minFinalColor, sizesForColor);
                            })
                            .toList();

                    List<SizeDTO> sizes = vars.stream()
                            .map(ProductVariant::getSize)
                            .filter(Objects::nonNull)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(Size::getId, sz -> sz, (s1, s2) -> s1),
                                    m -> m.values().stream()
                                            .map(sz -> new SizeDTO(sz.getId(), sz.getLabel()))
                                            .toList()
                            ));

                    return new ProductListDTO(
                            p.getId(),
                            p.getName(),
                            p.getImageUrl(),
                            minOriginal,   // price (gốc min)
                            minFinal,      // finalPrice (sau sale min)
                            onSale,        // onSale
                            colorPreviews,
                            sizes
                    );
                })
                .toList();

        // Lọc theo khoảng giá dựa trên finalPrice (fallback price)
        if (minPrice != null) {
            dtoList = dtoList.stream()
                    .filter(pd -> {
                        BigDecimal base = pd.getFinalPrice() != null ? pd.getFinalPrice() : pd.getPrice();
                        return base.compareTo(minPrice) >= 0;
                    })
                    .toList();
        }
        if (maxPrice != null) {
            dtoList = dtoList.stream()
                    .filter(pd -> {
                        BigDecimal base = pd.getFinalPrice() != null ? pd.getFinalPrice() : pd.getPrice();
                        return base.compareTo(maxPrice) <= 0;
                    })
                    .toList();
        }

        // Sort theo "price,asc|desc" (đang map sang property "dummy" ở Controller)
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            if ("dummy".equals(order.getProperty())) {
                Comparator<ProductListDTO> cmp = Comparator.comparing(pd ->
                        pd.getFinalPrice() != null ? pd.getFinalPrice() : pd.getPrice()
                );
                if (order.getDirection().isDescending()) cmp = cmp.reversed();
                dtoList = dtoList.stream().sorted(cmp).toList();
            }
        }

        // Phân trang thủ công
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
        // Nếu product.getVariants() có thể chưa load, cân nhắc fetch từ repo:
        // List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
        List<ProductVariant> variants = product.getVariants();

        // (Tuỳ chọn) chỉ tính trên variant đang bán
        var actives = variants.stream()
                .filter(v -> v != null && v.isActive())   // bỏ nếu muốn tính tất cả
                .toList();

        int totalQty = actives.stream()
                .map(v -> v.getQuantity() == null ? 0 : v.getQuantity())
                .mapToInt(Integer::intValue)
                .sum();

        // Base min/max (giá gốc)
        BigDecimal minPrice = actives.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = actives.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Effective min/max (đã trừ sale nếu đang hiệu lực)
        BigDecimal eMin = actives.stream()
                .map(ProductVariant::getEffectivePrice) // dùng helper đã có
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(minPrice);

        BigDecimal eMax = actives.stream()
                .map(ProductVariant::getEffectivePrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(maxPrice);

        return new ProductAdminDTO(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getGender() == 1 ? "Nam" : product.getGender() == 2 ? "Nữ" : "Unisex",
                product.getStatus(),
                product.getCreatedAt(),
                actives.size(),
                totalQty,
                minPrice,
                maxPrice,
                eMin,
                eMax
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

    public List<BestsellerDTO> getBestsellers(Integer days, int limit) {
        if (limit <= 0 || limit > 50) limit = 8;

        StringBuilder sql = new StringBuilder("""
            SELECT 
                p.id           AS product_id,
                p.name         AS name,
                SUM(oi.quantity) AS total_sold
            FROM order_item oi
            JOIN [order] o           ON o.id = oi.order_id
            JOIN product_variant pv  ON pv.id = oi.variant_id AND pv.deleted = 0
            JOIN product p           ON p.id = pv.product_id
            WHERE o.status IN ('DELIVERED')
        """);

        if (days != null) {
            sql.append(" AND o.created_at >= DATEADD(DAY, -:days, GETDATE()) ");
        }
        sql.append("""
            GROUP BY p.id, p.name
            ORDER BY total_sold DESC, p.id DESC
            OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
        """);

        var q = em.createNativeQuery(sql.toString());
        if (days != null) q.setParameter("days", days);
        q.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<BestsellerDTO> result = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            String name = (String) r[1];
            Long total = ((Number) r[2]).longValue();
            result.add(new BestsellerDTO(id, name, total));
        }
        return result;
    }
    public List<FavoriteDTO> getMostFavorited(int limit) {
        if (limit <= 0 || limit > 50) limit = 8;

        String sql = """
        SELECT p.id, p.name, COUNT(f.user_id) AS totalFavorites
        FROM favorite f
        JOIN product p ON p.id = f.product_id
        WHERE p.status = 'ACTIVE'
        GROUP BY p.id, p.name
        ORDER BY totalFavorites DESC, p.id DESC
        OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
    """;

        var q = em.createNativeQuery(sql);
        q.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<FavoriteDTO> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new FavoriteDTO(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    ((Number) r[2]).longValue()
            ));
        }
        return result;
    }
}
