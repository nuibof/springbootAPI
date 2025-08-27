// api/rest/SeasFit/controller/UserVoucherController.java
package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.entity.Voucher;
import api.rest.SeasFit.service.UserVoucherService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import api.rest.SeasFit.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user-vouchers")
@RequiredArgsConstructor
public class UserVoucherController {

    private final UserVoucherService userVoucherService;
    private final JwtUtil jwtUtil;
    private final api.rest.SeasFit.service.UserService userService;
    private final api.rest.SeasFit.repository.VoucherRepository voucherRepository;
    private final api.rest.SeasFit.repository.UserVoucherRepository userVoucherRepository;

    private User getAuthenticatedUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thiếu token xác thực");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        User user = userService.findByUserName(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại");
        }

        return user;
    }

    @PostMapping("/claim")
    public ResponseEntity<?> claim(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ClaimReq req) {

        if (authHeader == null || "Bearer null".equals(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập!"));
        }

        try {
            User user = getAuthenticatedUser(authHeader);

            // ==== validate voucher tồn tại ====
            Voucher voucher = voucherRepository.findByCode(req.getVoucherCode())
                    .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

            // ==== check active ====
            if (!Boolean.TRUE.equals(voucher.getIsActive())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Voucher đã bị vô hiệu hoá"));
            }

            // ==== check date ====
            LocalDateTime now = LocalDateTime.now();
            if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate().atStartOfDay())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Voucher chưa đến thời gian sử dụng"));
            }
            if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate().atStartOfDay())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Voucher đã hết hạn"));
            }

            // ==== check quantity ====
            if (voucher.getQuantity() != null && voucher.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Voucher đã hết lượt phát hành"));
            }

            // ==== check user đã có chưa ====
            if (userVoucherRepository.existsByUser_IdAndVoucher_Code(user.getId(), voucher.getCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã có voucher này trong ví rồi"));
            }

            // ==== nếu ok thì claim ====
            userVoucherService.claim(user.getId(), voucher.getCode());
            return ResponseEntity.ok(Map.of("message", "Đã thêm voucher vào ví"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/my")
    public ResponseEntity<?> myVouchers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || "Bearer null".equals(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập!"));
        }
        User user = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(userVoucherService.listForUser(user.getId()));
    }

    @DeleteMapping("/revoke/{code}")
    public ResponseEntity<?> revoke(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String code) {

        if (authHeader == null || "Bearer null".equals(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập!"));
        }
        User user = getAuthenticatedUser(authHeader);
        userVoucherService.revoke(user.getId(), code);
        return ResponseEntity.ok(Map.of("message", "Đã xoá voucher khỏi ví"));
    }

    @Data
    public static class ClaimReq { private String voucherCode; }
}
