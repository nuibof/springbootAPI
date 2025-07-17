package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
