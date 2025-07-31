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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
        System.out.println("Login attempt: Username=" + request.getUserName() + ", Password=" + request.getPassword());

        User user = userService.findByUserName(request.getUserName());

        if (user == null) {
            System.out.println("User not found: " + request.getUserName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tên đăng nhập không tồn tại.");
        }

        System.out.println("Expected: [" + user.getPassword() + "] Received: [" + request.getPassword() + "]");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            System.out.println("Incorrect password for user: " + request.getUserName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mật khẩu không đúng.");
        }

        String token = jwtUtil.generateToken(user.getUserName(), user.getRole());
        System.out.println("Login successful for user: " + user.getFullName());

        ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(18000)
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie clearJwt = ResponseCookie.from("jwtToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
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
        user.setCreatedAt(LocalDateTime.now());
        user.setRole("ROLE_USER");
        userService.save(user);
        System.out.println("User registered: " + user.getUserName());

        return ResponseEntity.ok("Đăng ký thành công.");
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@CookieValue(value = "jwtToken", required = false) String token) {
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
