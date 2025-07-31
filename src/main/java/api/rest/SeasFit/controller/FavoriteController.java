package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.FavoriteService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleFavorite(
            @CookieValue(value = "jwtToken", required = false) String token,
            @RequestParam Long productId
    ) {
        User user = getUserFromToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập.");
        }

        boolean liked = favoriteService.toggleFavorite(user.getId(), productId);
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/status")
    public ResponseEntity<?> isFavorited(
            @CookieValue(value = "jwtToken", required = false) String token,
            @RequestParam Long productId
    ) {
        User user = getUserFromToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập.");
        }

        boolean liked = favoriteService.isFavorited(user.getId(), productId);
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getFavoriteCount(@RequestParam Long productId) {
        long count = favoriteService.getFavoriteCount(productId);
        return ResponseEntity.ok(count);
    }

    private User getUserFromToken(String token) {
        if (token == null || token.isEmpty()) return null;
        String username = jwtUtil.extractUsername(token);
        if (username == null) return null;
        return userService.findByUserName(username);
    }
}
