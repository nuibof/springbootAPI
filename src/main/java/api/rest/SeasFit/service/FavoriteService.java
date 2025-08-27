package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.FavoriteAdminDTO;
import api.rest.SeasFit.dto.FavoriteDTO;
import api.rest.SeasFit.dto.FavoriteItemDTO;
import api.rest.SeasFit.entity.Favorite;
import api.rest.SeasFit.entity.Product;
import api.rest.SeasFit.entity.ProductVariant;
import api.rest.SeasFit.repository.FavoriteRepository;
import api.rest.SeasFit.repository.ProductRepository;
import api.rest.SeasFit.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    @PersistenceContext
    private EntityManager em;
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    // ===== CRUD đơn giản (giữ nguyên) =====

    public List<Favorite> findAll() {
        return favoriteRepository.findAll();
    }

    public Optional<Favorite> findById(Long id) {
        return favoriteRepository.findById(id);
    }

    public Favorite save(Favorite entity) {
        return favoriteRepository.save(entity);
    }

    public void deleteById(Long id) {
        favoriteRepository.deleteById(id);
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long productId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite fav = new Favorite(null, userId, productId, LocalDateTime.now());
            favoriteRepository.save(fav);
            return true;
        }
    }

    public boolean isFavorited(Long userId, Long productId) {
        return favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    public long getFavoriteCount(Long productId) {
        return favoriteRepository.countByProductId(productId);
    }

    @Transactional(readOnly = true)
    public Page<FavoriteAdminDTO> pageFavoriteStats(String q, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        return productRepository.pageFavoriteStats(q, pageable);
    }

    // ====== Mới: danh sách SP yêu thích theo DTO (chỉ dùng ảnh chính từ product) ======

    @Transactional(readOnly = true)
    public List<FavoriteItemDTO> listFavorites(Long userId) {
        List<Favorite> favs = favoriteRepository.findByUserId(userId);
        if (favs.isEmpty()) return Collections.emptyList();

        List<Long> productIds = favs.stream()
                .map(Favorite::getProductId)
                .distinct()
                .toList();

        // Lấy products & variants
        List<Product> products = productRepository.findAllById(productIds);

        Map<Long, List<ProductVariant>> variantsByProduct = productVariantRepository
                .findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        LocalDateTime now = LocalDateTime.now();

        return products.stream()
                .map(p -> toFavoriteDTO(p, variantsByProduct.getOrDefault(p.getId(), List.of()), now))
                .toList();
    }

    // ===== Helpers =====

    private FavoriteItemDTO toFavoriteDTO(Product p, List<ProductVariant> variants, LocalDateTime now) {
        // Min original price
        BigDecimal minOriginal = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        // Variant có finalPrice nhỏ nhất
        ProductVariant minVar = variants.stream()
                .min(Comparator.comparing(v -> effectivePrice(v, now)))
                .orElse(null);

        BigDecimal minFinal = (minVar != null) ? effectivePrice(minVar, now) : minOriginal;

        // Mức giảm áp dụng tại biến thể min
        BigDecimal appliedSale = BigDecimal.ZERO;
        if (minVar != null && minVar.getPrice() != null) {
            appliedSale = minVar.getPrice().subtract(minFinal);
            if (appliedSale.compareTo(BigDecimal.ZERO) < 0) {
                appliedSale = BigDecimal.ZERO;
            }
        }

        boolean onSale = minFinal.compareTo(minOriginal) < 0;

        return FavoriteItemDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .imageUrl(p.getImageUrl())          // <-- chỉ dùng ảnh chính từ bảng product
                .price(minOriginal)
                .finalPrice(minFinal)
                .onSale(onSale)
                .saleAmount(appliedSale)
                .build();
    }

    private BigDecimal effectivePrice(ProductVariant v, LocalDateTime now) {
        BigDecimal base = nonNull(v.getPrice());
        BigDecimal sale = nonNull(v.getSaleAmount());

        boolean activeSale =
                sale.compareTo(BigDecimal.ZERO) > 0 &&
                        (v.getSaleFrom() == null || !v.getSaleFrom().isAfter(now)) &&
                        (v.getSaleTo() == null   || v.getSaleTo().isAfter(now));

        if (!activeSale) return base;

        BigDecimal finalP = base.subtract(sale);
        return finalP.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalP;
    }

    private BigDecimal nonNull(BigDecimal b) {
        return b != null ? b : BigDecimal.ZERO;
    }
}
