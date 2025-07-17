package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Color;
import api.rest.SeasFit.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    public List<Color> findAll() {
        return colorRepository.findAll();
    }

    public Optional<Color> findById(Long id) {
        return colorRepository.findById(id);
    }

    public Color save(Color entity) {
        return colorRepository.save(entity);
    }

    public void deleteById(Long id) {
        colorRepository.deleteById(id);
    }
}
