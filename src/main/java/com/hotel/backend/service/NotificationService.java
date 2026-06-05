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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** Gửi thông báo đến chủ khách sạn (ownerId) khi khách thanh toán 70 % */
    @Transactional
    public void notifyOwner(Long ownerId, Long bookingId, String bookingCode, java.math.BigDecimal remainingAmount) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) return;

        String msg = String.format(
                "Khách đã thanh toán 70%% số dư (%.0f ₫) cho đơn đặt phòng #%s.",
                remainingAmount, bookingCode);

        Notification notification = Notification.builder()
                .receiver(owner)
                .message(msg)
                .bookingId(bookingId)
                .bookingCode(bookingCode)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
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
