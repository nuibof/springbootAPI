package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.Size;
import api.rest.SeasFit.service.SizeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sizes")
public class SizeController {
    final private SizeService sizeService;
    public SizeController(SizeService sizeService) {
        this.sizeService = sizeService;
    }
    @GetMapping("")
    public Object getAllSizes() {
        return sizeService.getAllSizes();
    }

    @PostMapping("/add")
    public ResponseEntity<?> addSize(@RequestBody Size size) {
        if (size.getLabel() == null || size.getLabel().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên size không được để trống");
        }
        if (sizeService.existsByLabel(size.getLabel())) {
            return ResponseEntity.badRequest().body("Size đã tồn tại");
        }
        return ResponseEntity.ok(sizeService.save(size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSize(@PathVariable Long id) {
        if (!sizeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        sizeService.deleteById(id);
        return ResponseEntity.ok("Size đã được xóa thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSize(@PathVariable Long id, @RequestBody Size size) {
        if (sizeService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (size.getLabel() == null || size.getLabel().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên size không được để trống");
        }
        Size sz = new Size();
        sz.setId(id.intValue());
        sz.setLabel(size.getLabel());
        System.out.println(sz);
        return ResponseEntity.ok(sizeService.save(sz));
    }

}
