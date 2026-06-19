package com.hotel.backend.controller;

import com.hotel.backend.dto.AdminBookingResponse;
import com.hotel.backend.service.AdminBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<List<AdminBookingResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminBookingService.getAll(keyword, status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminBookingResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminBookingService.updateStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        adminBookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDelete(@RequestBody List<Long> ids) {
        adminBookingService.bulkDeleteBookings(ids);
        return ResponseEntity.noContent().build();
    }
}
