package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Size;
import api.rest.SeasFit.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;

    public List<Size> findAll() {
        return sizeRepository.findAll();
    }

    public Optional<Size> findById(Long id) {
        return sizeRepository.findById(id);
    }

    public Size save(Size entity) {
        return sizeRepository.save(entity);
    }

    public void deleteById(Long id) {
        sizeRepository.deleteById(id);
    }

    public Object getAllSizes() {
        List<Size> sizes = sizeRepository.findAll();
        if (sizes.isEmpty()) {
            return "No sizes found";
        }
        return sizes;
    }
}
