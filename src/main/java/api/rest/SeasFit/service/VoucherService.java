package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.VoucherRequest;
import api.rest.SeasFit.dto.VoucherResponse;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.repository.VoucherRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoucherService {
    @PersistenceContext
    private EntityManager em;
    private final VoucherRepository voucherRepository;

    public List<VoucherResponse> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findAll();
        return vouchers.stream().map(VoucherResponse::fromEntity).toList();
    }

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public Voucher createVoucher(VoucherRequest req) {
        if (voucherRepository.existsByCode(req.getCode())) {
            throw new RuntimeException("Mã giảm giá đã tồn tại");
        }else if (req.getDiscountValue() <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        } else if (req.getMinOrderAmount() < 0) {
            throw new RuntimeException("Giá trị đơn hàng tối thiểu không thể âm");
        } else if (req.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng mã giảm giá phải lớn hơn 0");
        } else if (LocalDate.parse(req.getStartDate()).isAfter(LocalDate.parse(req.getEndDate()))) {
            throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
        } else if (LocalDate.parse(req.getStartDate()).isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày bắt đầu không thể trước ngày hiện tại");
        } else if (LocalDate.parse(req.getEndDate()).isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày kết thúc không thể trước ngày hiện tại");
        } else if (req.getDiscountType() == null || (!req.getDiscountType().equals("fixed") && !req.getDiscountType().equals("percent"))) {
            throw new RuntimeException("Loại giảm giá không hợp lệ");
        } else if (req.getCode() == null || req.getCode().isEmpty()) {
            throw new RuntimeException("Mã giảm giá không được để trống");
        } else if(req.getMaxDiscountValue() != null && req.getMaxDiscountValue() < 0) {
            throw new RuntimeException("Giá trị giảm giá tối đa không thể âm");
        }

        Voucher voucher = Voucher.builder()
                .code(req.getCode())
                .discountValue(BigDecimal.valueOf(req.getDiscountValue()))
                .discountType(req.getDiscountType())
                .description(req.getDescription())
                .minOrderAmount(BigDecimal.valueOf(req.getMinOrderAmount()))
                .quantity(req.getQuantity())
                .maxDiscountValue(BigDecimal.valueOf(req.getMaxDiscountValue()))
                .startDate(LocalDate.parse(req.getStartDate()))
                .endDate(LocalDate.parse(req.getEndDate()))
                .isActive(req.getIsActive() != null && req.getIsActive())
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        return voucherRepository.save(voucher);
    }

    public Voucher updateVoucher(VoucherRequest request) {
        Voucher voucher = voucherRepository.findByCode(request.getCode())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
        if (request.getDiscountValue() <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        } else if (request.getMinOrderAmount() < 0) {
            throw new RuntimeException("Giá trị đơn hàng tối thiểu không thể âm");
        } else if (request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng mã giảm giá phải lớn hơn 0");
        } else if (LocalDate.parse(request.getStartDate()).isAfter(LocalDate.parse(request.getEndDate()))) {
            throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
        } else if (LocalDate.parse(request.getEndDate()).isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày kết thúc không thể trước ngày hiện tại");
        } else if (request.getDiscountType() == null || (!request.getDiscountType().equals("fixed") && !request.getDiscountType().equals("percent"))) {
            throw new RuntimeException("Loại giảm giá không hợp lệ");
        } else if (request.getCode() == null || request.getCode().isEmpty()) {
            throw new RuntimeException("Mã giảm giá không được để trống");
        } else if(request.getMaxDiscountValue() != null && request.getMaxDiscountValue() < 0) {
            throw new RuntimeException("Giá trị giảm giá tối đa không thể âm");
        }

        try{
            voucher.setCode(request.getCode());
            voucher.setDiscountValue(BigDecimal.valueOf(request.getDiscountValue()));
            voucher.setDiscountType(request.getDiscountType());
            voucher.setDescription(request.getDescription());
            voucher.setMinOrderAmount(BigDecimal.valueOf(request.getMinOrderAmount()));
            voucher.setQuantity(request.getQuantity());
            voucher.setMaxDiscountValue(BigDecimal.valueOf(request.getMaxDiscountValue()));
            voucher.setStartDate(LocalDate.parse(request.getStartDate()));
            voucher.setEndDate(LocalDate.parse(request.getEndDate()));
            voucher.setIsActive(request.getIsActive() != null && request.getIsActive());
            voucher.setUpdatedAt(LocalDate.now());

            return voucherRepository.save(voucher);
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật mã giảm giá thất bại: " + e.getMessage());
        }
    }

    public void deleteVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
        try {
            voucherRepository.delete(voucher);
        } catch (Exception e) {
            throw new RuntimeException("Xóa mã giảm giá thất bại: " + e.getMessage());
        }
    }

    public Long countVouchers() {
        return voucherRepository.count();
    }

    public List<Map<String,Object>> getActiveVouchers() {
        var q = em.createNativeQuery("""
        SELECT id, code, description,
               discount_type, discount_value, max_discount_value,
               min_order_amount, quantity, 
               start_date, end_date
        FROM voucher
        WHERE is_active = 1
          AND quantity > 0
          AND (start_date IS NULL OR start_date <= GETDATE())
          AND (end_date IS NULL OR end_date >= GETDATE())
        ORDER BY end_date ASC
    """);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            var m = new LinkedHashMap<String,Object>();
            m.put("id", ((Number) r[0]).longValue());
            m.put("code", (String) r[1]);
            m.put("description", (String) r[2]);
            m.put("type", (String) r[3]);               // AMOUNT hoặc PERCENT
            m.put("value", ((Number) r[4]).longValue()); // số tiền hoặc %
            m.put("maxValue", r[5] == null ? null : ((Number) r[5]).longValue());
            m.put("minOrder", r[6] == null ? null : ((Number) r[6]).longValue());
            m.put("quantity", ((Number) r[7]).intValue());
            m.put("startDate", String.valueOf(r[8]));
            m.put("endDate", String.valueOf(r[9]));
            out.add(m);
        }
        return out;
    }
}
