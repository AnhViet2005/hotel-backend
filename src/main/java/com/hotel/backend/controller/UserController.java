package com.hotel.backend.controller;

import com.hotel.backend.dto.UserBookingResponse;
import com.hotel.backend.model.Booking;
import com.hotel.backend.model.HotelImage;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.service.UserBookingService;
import com.hotel.backend.dto.BookingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Long> createBooking(@RequestBody BookingRequest request, @AuthenticationPrincipal UserDetails userDetails) {
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

    @GetMapping("/bookings")
    public ResponseEntity<List<UserBookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<UserBookingResponse> result = bookings.stream().map(b -> {
            String hotelName = "N/A";
            String hotelImage = null;
            String hotelCity = null;

            try {
                if (b.getHotel() != null) {
                    hotelName = b.getHotel().getHotelName();
                    hotelCity = b.getHotel().getCity();
                    if (b.getHotel().getImages() != null) {
                        hotelImage = b.getHotel().getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                                .map(HotelImage::getImageUrl)
                                .findFirst()
                                .orElse(null);
                        if (hotelImage == null && !b.getHotel().getImages().isEmpty()) {
                            hotelImage = b.getHotel().getImages().iterator().next().getImageUrl();
                        }
                    }
                }
            } catch (Exception e) {
                // Hotel has been deleted or lazy loading issue
            }

            // Get room type name from booking rooms
            String roomTypeName = null;
            try {
                if (b.getBookingRooms() != null && !b.getBookingRooms().isEmpty()) {
                    roomTypeName = b.getBookingRooms().stream()
                            .findFirst()
                            .map(br -> br.getRoomType() != null ? br.getRoomType().getTypeName() : null)
                            .orElse(null);
                }
            } catch (Exception e) {
                // Lazy loading issue on booking rooms or room type
            }

            java.math.BigDecimal amount = b.getTotalAmount() != null ? b.getTotalAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal deposit = amount.multiply(new java.math.BigDecimal("0.3"));
            java.math.BigDecimal remaining = amount.multiply(new java.math.BigDecimal("0.7"));

            return UserBookingResponse.builder()
                    .id(b.getId())
                    .bookingCode(b.getBookingCode())
                    .hotelName(hotelName)
                    .hotelImage(hotelImage)
                    .hotelCity(hotelCity)
                    .roomTypeName(roomTypeName)
                    .checkIn(b.getCheckIn())
                    .checkOut(b.getCheckOut())
                    .totalAmount(amount)
                    .depositAmount(deposit)
                    .remainingAmount(remaining)
                    .status(b.getStatus() != null ? b.getStatus().name() : "UNKNOWN")
                    .remainingPaymentStatus(b.getRemainingPaymentStatus() != null ? b.getRemainingPaymentStatus().name() : "UNPAID")
                    .remainingPaymentMethod(b.getRemainingPaymentMethod() != null ? b.getRemainingPaymentMethod().name() : null)
                    .createdAt(b.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

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
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/settings", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSettings(
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Mật khẩu hiện tại không đúng."));
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        if (avatar != null && !avatar.isEmpty()) {
            String filename = fileUploadService.store(avatar);
            user.setAvatarUrl("http://localhost:8080/uploads/" + filename);
        }

        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of("message", "Cập nhật thành công", "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""));
    }
}
