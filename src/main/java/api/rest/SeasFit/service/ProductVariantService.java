package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.ProductVariant;
import api.rest.SeasFit.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;

    public List<ProductVariant> findAll() {
        return productVariantRepository.findAll();
    }

    public Optional<ProductVariant> findById(Long id) {
        return productVariantRepository.findById(id);
    }

    public ProductVariant save(ProductVariant entity) {
        return productVariantRepository.save(entity);
    }

    public void deleteById(Long id) {
        productVariantRepository.deleteById(id);
    }


}
