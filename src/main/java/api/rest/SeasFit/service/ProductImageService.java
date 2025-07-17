package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.ProductImage;
import api.rest.SeasFit.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;

    public List<ProductImage> findAll() {
        return productImageRepository.findAll();
    }

    public Optional<ProductImage> findById(Long id) {
        return productImageRepository.findById(id);
    }

    public ProductImage save(ProductImage entity) {
        return productImageRepository.save(entity);
    }

    public void deleteById(Long id) {
        productImageRepository.deleteById(id);
    }
}
