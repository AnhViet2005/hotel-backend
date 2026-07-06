package com.hotel.backend.controller;

import com.hotel.backend.dto.DashboardStatsResponse;
import com.hotel.backend.dto.RecentBookingResponse;
import com.hotel.backend.dto.RevenueBreakdownResponse;
import com.hotel.backend.model.Booking;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import com.hotel.backend.model.User;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;


    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isAdmin(User user) {
        if (user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return "ADMIN".equalsIgnoreCase(role) || "HỆ THỐNG".equalsIgnoreCase(role);
    }

    private boolean isOwnerOfBooking(Booking b, Long ownerId) {
        try {
            return b.getHotel() != null && b.getHotel().getOwner() != null && b.getHotel().getOwner().getId().equals(ownerId);
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardStatsResponse> getStats() {
        User current = getCurrentUser();
        List<Booking> bookings = bookingRepository.findAll();

        if (!isAdmin(current)) {
            bookings = bookings.stream()
                    .filter(b -> isOwnerOfBooking(b, current.getId()))
                    .collect(Collectors.toList());
        }
        
        java.math.BigDecimal totalRevenue;
        if (isAdmin(current)) {
            // Admin receives 30% deposit of all CONFIRMED or COMPLETED bookings
            totalRevenue = bookings.stream()
                    .filter(b -> b.getStatus() != null && 
                            ("CONFIRMED".equals(b.getStatus().name()) || "COMPLETED".equals(b.getStatus().name())))
                    .map(b -> b.getAdminRevenue() != null ? b.getAdminRevenue() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        } else {
            // Hotel Owner receives remaining 70% of COMPLETED bookings (room stay finished)
            totalRevenue = bookings.stream()
                    .filter(b -> b.getStatus() != null && "COMPLETED".equals(b.getStatus().name()))
                    .map(b -> b.getHotelOwnerRevenue() != null ? b.getHotelOwnerRevenue() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }
        
        long totalBookings = bookings.size();
        long totalCustomers;
        long totalHotels;

        if (isAdmin(current)) {
            totalCustomers = userRepository.count();
            totalHotels = hotelRepository.count();
        } else {
            totalCustomers = bookings.stream()
                    .map(Booking::getGuestEmail)
                    .filter(java.util.Objects::nonNull)
                    .filter(email -> !email.isBlank())
                    .distinct()
                    .count();
            totalHotels = hotelRepository.findAll().stream()
                    .filter(h -> {
                        try {
                            return h.getOwner() != null && h.getOwner().getId().equals(current.getId());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
        }

        return ResponseEntity.ok(DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .totalCustomers(totalCustomers)
                .totalHotels(totalHotels)
                .build());
    }

    @GetMapping("/recent-bookings")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RecentBookingResponse>> getRecentBookings() {
        User current = getCurrentUser();
        List<Booking> bookings;

        if (isAdmin(current)) {
            bookings = bookingRepository.findAll(
                    PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        } else {
            bookings = bookingRepository.findAll().stream()
                    .filter(b -> isOwnerOfBooking(b, current.getId()))
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(5)
                    .collect(Collectors.toList());
        }

        List<RecentBookingResponse> response = bookings.stream()
                .map(b -> {
                    String hotelName = "N/A";
                    try {
                        if (b.getHotel() != null) {
                            hotelName = b.getHotel().getHotelName();
                        }
                    } catch (Exception e) {
                        // Hotel may have been deleted - ignore
                    }
                    String statusStr = "UNKNOWN";
                    if (b.getStatus() != null) {
                        statusStr = b.getStatus().toString();
                    }
                    
                    java.math.BigDecimal amount = b.getTotalAmount() != null ? b.getTotalAmount() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal deposit = b.getAdminRevenue() != null ? b.getAdminRevenue() : amount.multiply(new java.math.BigDecimal("0.3"));
                    java.math.BigDecimal remaining = b.getHotelOwnerRevenue() != null ? b.getHotelOwnerRevenue() : amount.multiply(new java.math.BigDecimal("0.7"));

                    return RecentBookingResponse.builder()
                            .id(b.getBookingCode())
                            .customerName(b.getGuestName())
                            .hotelName(hotelName)
                            .bookingDate(b.getCreatedAt())
                            .amount(amount)
                            .depositAmount(deposit)
                            .remainingAmount(remaining)
                            .status(statusStr)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-breakdown")
    @Transactional(readOnly = true)
    public ResponseEntity<RevenueBreakdownResponse> getRevenueBreakdown() {
        User current = getCurrentUser();
        List<Booking> bookings = bookingRepository.findAll();

        if (!isAdmin(current)) {
            bookings = bookings.stream()
                    .filter(b -> isOwnerOfBooking(b, current.getId()))
                    .collect(Collectors.toList());
        }

        java.math.BigDecimal totalAmount = bookings.stream()
                .filter(b -> b.getStatus() != null && 
                        ("CONFIRMED".equals(b.getStatus().name()) || "COMPLETED".equals(b.getStatus().name())))
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal adminRevenue;
        java.math.BigDecimal hotelOwnerRevenue;

        if (isAdmin(current)) {
            adminRevenue = bookings.stream()
                    .filter(b -> b.getStatus() != null && 
                            ("CONFIRMED".equals(b.getStatus().name()) || "COMPLETED".equals(b.getStatus().name())))
                    .map(b -> b.getAdminRevenue() != null ? b.getAdminRevenue() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            hotelOwnerRevenue = bookings.stream()
                    .filter(b -> b.getStatus() != null && "COMPLETED".equals(b.getStatus().name()))
                    .map(b -> b.getHotelOwnerRevenue() != null ? b.getHotelOwnerRevenue() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        } else {
            // Hotel owner only sees their revenue
            adminRevenue = java.math.BigDecimal.ZERO;
            hotelOwnerRevenue = bookings.stream()
                    .filter(b -> b.getStatus() != null && "COMPLETED".equals(b.getStatus().name()))
                    .map(b -> b.getHotelOwnerRevenue() != null ? b.getHotelOwnerRevenue() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }

        return ResponseEntity.ok(RevenueBreakdownResponse.builder()
                .totalAmount(totalAmount)
                .adminRevenue(adminRevenue)
                .hotelOwnerRevenue(hotelOwnerRevenue)
                .revenueDistribution("Admin: 30% | Hotel Owner: 70%")
                .build());
    }
    
}
