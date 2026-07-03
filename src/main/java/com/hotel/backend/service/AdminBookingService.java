package com.hotel.backend.service;

import com.hotel.backend.dto.AdminBookingResponse;
import com.hotel.backend.model.Booking;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isAdmin(User user) {
        if (user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return "ADMIN".equalsIgnoreCase(role) || "HỆ THỐNG".equalsIgnoreCase(role);
    }

    private boolean isOwnerOfBooking(Booking b, Long ownerId) {
        try {
            return b.getHotel() != null && b.getHotel().getOwner() != null
                    && b.getHotel().getOwner().getId().equals(ownerId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesKeyword(Booking b, String kw) {
        try {
            boolean codeMatches = b.getBookingCode() != null && b.getBookingCode().toLowerCase().contains(kw);
            boolean guestMatches = b.getGuestName() != null && b.getGuestName().toLowerCase().contains(kw);
            boolean hotelMatches = b.getHotel() != null && b.getHotel().getHotelName() != null
                    && b.getHotel().getHotelName().toLowerCase().contains(kw);
            return codeMatches || guestMatches || hotelMatches;
        } catch (Exception e) {
            return (b.getBookingCode() != null && b.getBookingCode().toLowerCase().contains(kw))
                    || (b.getGuestName() != null && b.getGuestName().toLowerCase().contains(kw));
        }
    }

    // ──────────────────────────────────────────────
    // Get All Bookings
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAll(String keyword, String status) {
        User current = getCurrentUser();
        List<Booking> bookings = bookingRepository.findAll();

        boolean admin = isAdmin(current);
        if (!admin) {
            bookings = bookings.stream()
                    .filter(b -> isOwnerOfBooking(b, current.getId()))
                    .collect(Collectors.toList());
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            bookings = bookings.stream().filter(b -> matchesKeyword(b, kw)).collect(Collectors.toList());
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            bookings = bookings.stream()
                    .filter(b -> b.getStatus() != null && b.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        bookings.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return bookings.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Update Status (Admin xác nhận cọc → COMPLETED)
    // ──────────────────────────────────────────────

    @Transactional
    public AdminBookingResponse updateStatus(Long id, String status) {
        User current = getCurrentUser();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt phòng."));

        if (!isAdmin(current) && !isOwnerOfBooking(booking, current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật trạng thái.");
        }

        try {
            Booking.BookingStatus newStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
            
            // Nếu hủy đặt phòng thì đặt doanh thu về 0
            if (newStatus == Booking.BookingStatus.CANCELLED) {
                booking.setAdminRevenue(java.math.BigDecimal.ZERO);
                booking.setHotelOwnerRevenue(java.math.BigDecimal.ZERO);
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
        }

        Booking saved = bookingRepository.save(booking);
        // Gửi thông báo cho khách hàng và Admin/Chủ khách sạn
        try {
            // 1. Thông báo cho khách hàng khi Admin thay đổi trạng thái
            if (saved.getStatus() == Booking.BookingStatus.CONFIRMED) {
                notificationService.notifyBookingConfirmed(saved);
            } else if (saved.getStatus() == Booking.BookingStatus.CANCELLED) {
                notificationService.notifyBookingCancelled(saved, "Admin đã cập nhật trạng thái đơn hàng.");
            } else if (saved.getStatus() == Booking.BookingStatus.COMPLETED) {
                notificationService.sendToUser(saved.getUser(), 
                    String.format("Đơn hàng #%s tại %s đã hoàn tất. Cảm ơn bạn đã sử dụng dịch vụ!", 
                        saved.getBookingCode(), saved.getHotel().getHotelName()),
                    saved.getId(), saved.getBookingCode(), "BOOKING_COMPLETED");
            }

            // 2. Thông báo cho Admin/Chủ khách sạn (giữ nguyên logic cũ)
            if (saved.getHotel() != null) {
                Long ownerId = saved.getHotel().getOwner() != null ? saved.getHotel().getOwner().getId() : null;
                if (saved.getStatus() == Booking.BookingStatus.CONFIRMED) {
                    notificationService.notifyDepositReceived(
                        saved.getId(),
                        saved.getBookingCode(),
                        saved.getAdminRevenue(),
                        ownerId
                    );
                } else if (saved.getStatus() == Booking.BookingStatus.COMPLETED) {
                    notificationService.notifyFullPaymentReceived(
                        saved.getId(),
                        saved.getBookingCode(),
                        saved.getTotalAmount(),
                        ownerId
                    );
                }
            }
        } catch (Exception ignored) {}

        return toResponse(saved);
    }

    @Transactional
    public void deleteBooking(Long id) {
        User current = getCurrentUser();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt phòng."));

        if (!isAdmin(current) && !isOwnerOfBooking(booking, current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xóa đơn đặt phòng này.");
        }

        // Chỉ cho phép xóa đơn đã hủy hoặc đã hoàn tất
        if (booking.getStatus() != Booking.BookingStatus.CANCELLED && booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xóa đơn hàng đã Hủy hoặc Hoàn tất.");
        }

        bookingRepository.delete(booking);
    }

    @Transactional
    public void bulkDeleteBookings(List<Long> ids) {
        User current = getCurrentUser();
        List<Booking> bookings = bookingRepository.findAllById(ids);

        for (Booking booking : bookings) {
            if (!isAdmin(current) && !isOwnerOfBooking(booking, current.getId())) {
                continue; // Skip if no permission
            }

            // Chỉ xóa đơn đã hủy hoặc hoàn tất
            if (booking.getStatus() == Booking.BookingStatus.CANCELLED || booking.getStatus() == Booking.BookingStatus.COMPLETED) {
                bookingRepository.delete(booking);
            }
        }
    }

    // ──────────────────────────────────────────────
    // toResponse
    // ──────────────────────────────────────────────

    public AdminBookingResponse toResponse(Booking b) {
        String hotelName = "N/A";
        try {
            if (b.getHotel() != null) hotelName = b.getHotel().getHotelName();
        } catch (Exception ignored) {}

        java.math.BigDecimal amount = b.getTotalAmount() != null ? b.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal deposit = amount.multiply(new java.math.BigDecimal("0.3"));
        java.math.BigDecimal remaining = amount.multiply(new java.math.BigDecimal("0.7"));

        return AdminBookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .customerName(b.getGuestName())
                .customerEmail(b.getGuestEmail())
                .hotelName(hotelName)
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .totalAmount(amount)
                .depositAmount(deposit)
                .remainingAmount(remaining)
                .status(b.getStatus() != null ? b.getStatus().name() : "UNKNOWN")
                .remainingPaymentStatus(b.getRemainingPaymentStatus() != null ? b.getRemainingPaymentStatus().name() : "UNPAID")
                .remainingPaymentMethod(b.getRemainingPaymentMethod() != null ? b.getRemainingPaymentMethod().name() : null)
                .remainingPaidAt(b.getRemainingPaidAt())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
