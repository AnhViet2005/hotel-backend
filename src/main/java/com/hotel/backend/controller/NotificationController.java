package com.hotel.backend.controller;

import com.hotel.backend.dto.NotificationResponse;
import com.hotel.backend.model.Notification;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User current = getCurrentUser(userDetails);
        List<Notification> notifications = notificationService.getForOwner(current.getId());
        List<NotificationResponse> response = notifications.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User current = getCurrentUser(userDetails);
        long count = notificationService.countUnread(current.getId());
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        User current = getCurrentUser(userDetails);
        notificationService.markAllRead(current.getId());
        return ResponseEntity.noContent().build();
    }

    private NotificationResponse toDto(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .bookingId(n.getBookingId())
                .bookingCode(n.getBookingCode())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
