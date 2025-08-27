package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.AddressRequest;
import api.rest.SeasFit.dto.AddressResponse;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.AddressService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

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

    @GetMapping
    public List<AddressResponse> getAll(@RequestHeader("Authorization") String authHeader) {
        User user = getAuthenticatedUser(authHeader);
        return addressService.getAllByUser(user.getId());
    }

    @PostMapping
    public AddressResponse create(@RequestHeader("Authorization") String authHeader, @RequestBody AddressRequest request) {
        User user = getAuthenticatedUser(authHeader);
        return addressService.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody AddressRequest request) {
        User user = getAuthenticatedUser(authHeader);
        return addressService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        User user = getAuthenticatedUser(authHeader);
        addressService.delete(user.getId(), id);
    }
}
