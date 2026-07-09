package com.hotel.backend.controller;

import com.hotel.backend.dto.AuthResponse;
import com.hotel.backend.dto.LoginRequest;
import com.hotel.backend.dto.RegisterRequest;
import com.hotel.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.hotel.backend.repository.UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<java.util.List<com.hotel.backend.model.User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerAdmin(request));
    }

    @PostMapping("/register-owner")
    public ResponseEntity<AuthResponse> registerOwner(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerOwner(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody com.hotel.backend.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("Yêu cầu khôi phục mật khẩu đã được gửi đến email của bạn.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody com.hotel.backend.dto.ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Mật khẩu đã được thay đổi thành công.");
    }
}
