package api.rest.SeasFit.controller;

import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.FavoriteService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private ResponseEntity<?> unauthorized(String msg) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

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

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleFavorite(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long productId
    ) {
        try {
            User user = getAuthenticatedUser(authHeader);
            boolean liked = favoriteService.toggleFavorite(user.getId(), productId);
            return ResponseEntity.ok(liked);
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> isFavorited(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long productId
    ) {
        try {
            User user = getAuthenticatedUser(authHeader);
            boolean liked = favoriteService.isFavorited(user.getId(), productId);
            return ResponseEntity.ok(liked);
        } catch (RuntimeException e) {
            return unauthorized(e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getFavoriteCount(@RequestParam Long productId) {
        long count = favoriteService.getFavoriteCount(productId);
        return ResponseEntity.ok(count);
    }
}
