package api.rest.SeasFit.controller;

import api.rest.SeasFit.service.UserService;
import org.springframework.web.bind.annotation.*;
import api.rest.SeasFit.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import api.rest.SeasFit.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(
            @CookieValue(value = "jwtToken", required = false) String cookieToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Lấy token từ header
        } else if (cookieToken != null) {
            token = cookieToken; // Fallback nếu frontend dùng cookie
        }

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có token xác thực.");
        }

        String userName = jwtUtil.extractUsername(token);
        if (userName == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ.");
        }

        User user = userService.findByUserName(userName);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        if ("Google".equals(user.getPassword())) {
            user.setPassword("!@#$Google!@#$");
        } else {
            user.setPassword(null);
        }

        return ResponseEntity.ok(user);
    }


    @PostMapping("/update")
    public ResponseEntity<?> updateUserInfo(
            @CookieValue(value = "jwtToken", required = false) String cookieToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody User updatedUser
    ) {
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (cookieToken != null) {
            token = cookieToken;
        }

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có token xác thực.");
        }

        String userName = jwtUtil.extractUsername(token);
        if (userName == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ.");
        }

        User existingUser = userService.findByUserName(userName);
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        if ("Google".equals(existingUser.getPassword())) {
            updatedUser.setFullName(existingUser.getFullName());
            updatedUser.setEmail(existingUser.getEmail());
        }

        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setGender(updatedUser.getGender());
        existingUser.setIdentityCard(updatedUser.getIdentityCard());
        existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());

        userService.save(existingUser);

        return ResponseEntity.ok(existingUser);
    }


}
