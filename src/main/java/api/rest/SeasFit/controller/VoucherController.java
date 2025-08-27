package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.VoucherRequest;
import api.rest.SeasFit.dto.VoucherResponse;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/admin/vouchers")
    public ResponseEntity<Voucher> createVoucher(@RequestBody VoucherRequest request) {
        Voucher saved = voucherService.createVoucher(request);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    @PutMapping("/admin/vouchers")
    public ResponseEntity<Voucher> updateVoucher(@RequestBody VoucherRequest request) {
        Voucher updated = voucherService.updateVoucher(request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/admin/vouchers/{code}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable String code) {
        voucherService.deleteVoucher(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("vouchers/count")
    public ResponseEntity<Long> countVouchers() {
        Long count = voucherService.countVouchers();
        return ResponseEntity.ok(count);
    }
}
