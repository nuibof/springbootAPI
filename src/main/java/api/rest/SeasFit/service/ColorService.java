package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Color;
import api.rest.SeasFit.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public List<Color> getAllColors() {
        List<Color> allColors = colorRepository.findAll();
        return allColors.stream()
                .filter(distinctByKey(Color::getHexCode))
                .collect(Collectors.toList());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
