// src/main/java/api/rest/SeasFit/service/PasswordOtpService.java
package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Password;
import api.rest.SeasFit.repository.PasswordRepository;
import api.rest.SeasFit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordOtpService {
    private final PasswordRepository tokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService; // gửi mail plain text
    private static final SecureRandom RND = new SecureRandom();

    private byte[] sha256(String s) {
        try { return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private String genOtp() { return String.format("%06d", RND.nextInt(1_000_000)); }

    /** B1: gửi OTP */
    @Transactional
    public void begin(String email) {
        userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Email không tồn tại"));
        String otp = genOtp();

        // lấy token active gần nhất hoặc tạo mới
        Password t = tokenRepo.findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, LocalDateTime.now())
                .orElseGet(Password::new);

        t.setEmail(email);
        t.setOtpHash(sha256(otp));
        t.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        t.setUsed(false);
        t.setAttempts(0);
        t.setCreatedAt(LocalDateTime.now());
        tokenRepo.save(t);

        // gửi mail đơn giản
        mailService.sendOtpMail(email, otp);
    }

    /** B2: kiểm OTP */
    @Transactional(readOnly = true)
    public void check(String email, String otp) {
        var t = tokenRepo.findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("OTP hết hạn hoặc không tồn tại"));
        if (t.getAttempts() >= 5) throw new RuntimeException("Thử OTP quá số lần cho phép");
        if (!MessageDigest.isEqual(t.getOtpHash(), sha256(otp))) throw new RuntimeException("OTP sai");
    }

    /** B3: đổi mật khẩu */
    @Transactional
    public void updatePassword(String email, String otp, String newRawPassword) {
        // 1. Lấy token hợp lệ
        Password token = tokenRepo.findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("OTP hết hạn hoặc không tồn tại"));

        // 2. Tăng số lần thử
        token.setAttempts(token.getAttempts() + 1);
        if (token.getAttempts() > 5) {
            tokenRepo.save(token);
            throw new RuntimeException("Thử OTP quá số lần cho phép");
        }

        // 3. Kiểm tra OTP
        if (!MessageDigest.isEqual(token.getOtpHash(), sha256(otp))) {
            tokenRepo.save(token); // lưu attempts tăng
            throw new RuntimeException("OTP sai");
        }

        // 4. Đánh dấu OTP đã dùng
        token.setUsed(true);
        tokenRepo.save(token);

        // 5. Cập nhật mật khẩu user
        updateUserPassword(email, newRawPassword);
    }

    /** hàm phụ: cập nhật mật khẩu user */
    private void updateUserPassword(String email, String newRawPassword) {
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepo.save(user);
    }

}
