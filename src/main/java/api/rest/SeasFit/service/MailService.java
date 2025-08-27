// src/main/java/api/rest/SeasFit/service/MailService.java
package api.rest.SeasFit.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@seasfit.vn}")
    private String from;

    public void sendText(String to, String subject, String body) {
        var msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    public void sendHtml(String to, String subject, String html) {
        try {
            var mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new RuntimeException("Không gửi được email", e);
        }
    }

    // Cái này vẫn giữ để OTP dùng nhanh
    public void sendOtpMail(String to, String otp) {
        sendText(to, "Mã OTP khôi phục mật khẩu",
                "Mã OTP của bạn: " + otp + "\nHiệu lực: 10 phút.");
    }
}

