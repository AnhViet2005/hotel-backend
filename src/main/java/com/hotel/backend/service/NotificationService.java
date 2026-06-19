package com.hotel.backend.service;

import com.hotel.backend.model.Notification;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.NotificationRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** Gửi thông báo khi khách thanh toán cọc 30% */
    @Transactional
    public void notifyDepositReceived(Long bookingId, String bookingCode, java.math.BigDecimal amount, Long hotelOwnerId) {
        // 1. Thông báo cho chủ khách sạn
        User owner = userRepository.findById(hotelOwnerId).orElse(null);
        if (owner != null) {
            String ownerMsg = String.format("Phòng của bạn đã được đặt! Khách đã thanh toán cọc 30%% (%s ₫) cho đơn #%s.",
                formatAmount(amount), bookingCode);
            saveNotification(owner, ownerMsg, bookingId, bookingCode);
        }

        // 2. Thông báo cho Admin (vì Admin giữ 30% cọc)
        userRepository.findAll().stream()
            .filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getRoleName()))
            .forEach(admin -> {
                String adminMsg = String.format("Hệ thống nhận được %s ₫ tiền cọc cho đơn hàng #%s.",
                    formatAmount(amount), bookingCode);
                saveNotification(admin, adminMsg, bookingId, bookingCode);
            });
    }

    /** Gửi thông báo khi hoàn tất thanh toán 100% (sau khi khách thanh toán 70% còn lại) */
    @Transactional
    public void notifyFullPaymentReceived(Long bookingId, String bookingCode, java.math.BigDecimal amount, Long hotelOwnerId) {
        User owner = userRepository.findById(hotelOwnerId).orElse(null);
        if (owner != null) {
            String msg = String.format("Tuyệt vời! Khách đã thanh toán đủ 100%% giá trị đơn hàng #%s (%s ₫).",
                bookingCode, formatAmount(amount));
            saveNotification(owner, msg, bookingId, bookingCode);
        }
    }

    /** Gửi thông báo đến chủ khách sạn (ownerId) khi khách thanh toán 70 % */
    @Transactional
    public void notifyOwner(Long ownerId, Long bookingId, String bookingCode, java.math.BigDecimal remainingAmount) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) return;

        String msg = String.format(
                "Khách đã thanh toán 70%% số dư (%s ₫) cho đơn đặt phòng #%s. Đơn hàng hiện đã hoàn tất thanh toán.",
                formatAmount(remainingAmount), bookingCode);

        saveNotification(owner, msg, bookingId, bookingCode);
    }

    private void saveNotification(User receiver, String message, Long bookingId, String bookingCode) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .message(message)
                .bookingId(bookingId)
                .bookingCode(bookingCode)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    /** Lấy tất cả thông báo của owner */
    public List<Notification> getForOwner(Long ownerId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(ownerId);
    }

    /** Lấy thông báo chưa đọc */
    public List<Notification> getUnreadForOwner(Long ownerId) {
        return notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(ownerId);
    }

    /** Đếm thông báo chưa đọc */
    public long countUnread(Long ownerId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(ownerId);
    }

    /** Đánh dấu đã đọc */
    @Transactional
    public void markRead(Long notifId, Long ownerId) {
        Notification n = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getReceiver().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    /** Đánh dấu tất cả đã đọc */
    @Transactional
    public void markAllRead(Long ownerId) {
        List<Notification> unread = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(ownerId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}
