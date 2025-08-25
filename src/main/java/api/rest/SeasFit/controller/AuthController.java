package api.rest.SeasFit.controller;

import api.rest.SeasFit.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import api.rest.SeasFit.security.AuthRequest;
import api.rest.SeasFit.security.AuthResponse;
import api.rest.SeasFit.security.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import api.rest.SeasFit.entity.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userDAO, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userDAO;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        // dùng logger thay vì System.out (khuyên xài @Slf4j)
        // log.info("Login attempt: {}", request.getUserName());

        final String username = request.getUserName();
        final String rawPassword = request.getPassword();

        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Thiếu username/password"));
        }

        User user = userService.findByUserName(username);
        if (user == null) {
            // log.warn("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Tên đăng nhập không tồn tại."));
        }

        // sai mật khẩu
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            // log.warn("Bad password for: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Mật khẩu không đúng."));
        }

        // khoá tài khoản
        String status = user.getStatus() == null ? "INACTIVE" : user.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(status)) {
            // log.warn("User locked: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Tài khoản bị đình chỉ!"));
        }

        // ok -> cấp token
        String token = jwtUtil.generateToken(user.getUserName(), user.getRole());

        // set httpOnly cookie để FE dùng credentials: 'include'
        ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", token)
                .httpOnly(true)
                .secure(true)          // bật HTTPS; dev http có thể tắt (không khuyến nghị)
                .sameSite("None")      // để chia sẻ cookie cross-site
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        // có thể trả thêm info user nếu cần FE hiển thị
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("userName", user.getUserName());
        userMap.put("fullName", user.getFullName());   // có thể null -> OK
        userMap.put("role", user.getRole());
        userMap.put("status", user.getStatus());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("user", userMap);

        return ResponseEntity.ok(body);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie clearJwt = ResponseCookie.from("jwtToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, clearJwt.toString());

        return ResponseEntity.ok("Đăng xuất thành công");
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        String error = userService.validateUser(user);
        if (error != null) return ResponseEntity.badRequest().body(error);

        System.out.println("Register attempt: Username=" + user.getUserName() +
                ", Email=" + user.getEmail() +
                ", Password=" + user.getPassword() +
                ", Phone=" + user.getPhone());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setRole("ROLE_USER");
        userService.save(user);
        System.out.println("User registered: " + user.getUserName());

        return ResponseEntity.ok("Đăng ký thành công.");
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(
            @CookieValue(value = "jwtToken", required = false) String cookieToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = null;

        // Ưu tiên header nếu có
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (cookieToken != null) {
            token = cookieToken;
        }

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Không phát hiện token"));
        }

        System.out.println("User with token: " + token + " is requesting user info");

        String userName = jwtUtil.extractUsername(token);
        User user = userService.findByUserName(userName);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy người dùng"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("role", user.getRole());
        data.put("userName", user.getUserName());
        data.put("fullName", user.getFullName());
        data.put("imageUrl", user.getImageUrl());

        return ResponseEntity.ok(data);
    }

}
