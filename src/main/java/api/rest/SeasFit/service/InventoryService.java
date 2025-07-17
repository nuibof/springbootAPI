package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Inventory;
import api.rest.SeasFit.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> findById(Long id) {
        return inventoryRepository.findById(id);
    }

    public Inventory save(Inventory entity) {
        return inventoryRepository.save(entity);
    }

    public void deleteById(Long id) {
        inventoryRepository.deleteById(id);
    }
}
