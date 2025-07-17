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

    // Lấy thông tin người dùng theo jwtToken
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@CookieValue(value = "jwtToken", required = false) String token) {
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

        if(user.getPassword().equals("Google"))
        {
            // Nếu người dùng đăng nhập bằng Google, trả về Google với ký tự đặc biệt
            user.setPassword("!@#$Google!@#$");
        } else {
            // Mã hóa lại mật khẩu trước khi trả về
            user.setPassword(null);
        }
        // Trả về thông tin người dùng
        return ResponseEntity.ok(user);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateUserInfo(
            @CookieValue(value = "jwtToken", required = false) String token,
            @RequestBody User updatedUser // <- BẮT BUỘC có cái này để ánh xạ JSON
    ) {
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

        // Nếu password là "Google" (không hash) thì không cho cập nhật email, fullName
        if ("Google".equals(existingUser.getPassword())) {
            updatedUser.setFullName(existingUser.getFullName());
            updatedUser.setEmail(existingUser.getEmail());
        }

        // Cập nhật thông tin cho các field khác
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setGender(updatedUser.getGender());
        existingUser.setIdentityCard(updatedUser.getIdentityCard());
        existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());

        userService.save(existingUser);

        return ResponseEntity.ok(existingUser);
    }

}
