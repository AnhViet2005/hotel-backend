package com.hotel.backend.controller;

import com.hotel.backend.dto.UserBookingResponse;
import com.hotel.backend.model.Booking;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.service.UserBookingService;
import com.hotel.backend.dto.BookingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import com.hotel.backend.service.FileUploadService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor

public class UserController {

    private final BookingRepository bookingRepository;
    private final UserBookingService userBookingService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/bookings")
    public ResponseEntity<Long> createBooking(@RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long bookingId = userBookingService.createBooking(request, userDetails.getUsername());
        return ResponseEntity.ok(bookingId);
    }

    @PatchMapping("/bookings/{id}/confirm")
    public ResponseEntity<Void> confirmBooking(@PathVariable Long id) {
        userBookingService.confirmBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/bookings/{id}/pay-remaining")
    public ResponseEntity<UserBookingResponse> payRemaining(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userBookingService.payRemaining(id, body.get("method")));
    }

    @PatchMapping("/bookings/{id}/cancel")
    public ResponseEntity<UserBookingResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(userBookingService.cancelBooking(id));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<UserBookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<UserBookingResponse> result = bookings.stream()
                .map(userBookingService::toUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> ResponseEntity.ok(java.util.Map.of(
                        "id", u.getId(),
                        "fullName", u.getFullName() != null ? u.getFullName() : "",
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "phone", u.getPhone() != null ? u.getPhone() : "",
                        "role", u.getRole() != null ? u.getRole().getRoleName() : "CUSTOMER",
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "")))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/settings", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSettings(
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        // Update basic info
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone.trim());
        }
        if (email != null && !email.trim().isEmpty() && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Email này đã được sử dụng bởi một tài khoản khác."));
            }
            user.setEmail(email.trim().toLowerCase());
        }

        // Update password
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Mật khẩu hiện tại không đúng."));
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        // Update avatar
        if (avatar != null && !avatar.isEmpty()) {
            String filename = fileUploadService.store(avatar);
            user.setAvatarUrl("http://localhost:8080/uploads/" + filename);
        }

        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Cập nhật thành công",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "email", user.getEmail()
        ));
    }
}
