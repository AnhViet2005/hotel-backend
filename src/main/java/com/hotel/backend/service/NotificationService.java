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
        // 1. Thông báo cho chủ khách sạn (Chỉ khách sạn của họ)
        if (hotelOwnerId != null) {
            User owner = userRepository.findById(hotelOwnerId).orElse(null);
            if (owner != null) {
                String ownerMsg = String.format("Phòng của bạn đã được đặt! Khách đã thanh toán cọc 30%% (%s ₫) cho đơn #%s.",
                    formatAmount(amount), bookingCode);
                saveNotification(owner, ownerMsg, bookingId, bookingCode, "DEPOSIT_PAID");
            }
        }

        // 2. Thông báo cho Admin chính qua Email
        userRepository.findByEmail("admin@hotel.com").ifPresent(admin -> {
            // Chỉ gửi nếu admin khác chủ khách sạn (tránh trùng)
            if (hotelOwnerId == null || !admin.getId().equals(hotelOwnerId)) {
                String msg = String.format("Bạn nhận được đơn đặt phòng mới và nhận được %s đ từ khách hàng cho đơn hàng #%s.",
                    formatAmount(amount), bookingCode);
                saveNotification(admin, msg, bookingId, bookingCode, "DEPOSIT_PAID");
            }
        });
    }

    /** Gửi thông báo khi hoàn tất thanh toán 100% (sau khi khách thanh toán 70% còn lại) */
    @Transactional
    public void notifyFullPaymentReceived(Long bookingId, String bookingCode, java.math.BigDecimal amount, Long hotelOwnerId) {
        // 1. Thông báo cho chủ khách sạn
        if (hotelOwnerId != null) {
            User owner = userRepository.findById(hotelOwnerId).orElse(null);
            if (owner != null) {
                String msg = String.format("Tuyệt vời! Khách đã thanh toán đủ 100%% giá trị đơn hàng #%s (%s ₫).",
                    bookingCode, formatAmount(amount));
                saveNotification(owner, msg, bookingId, bookingCode, "FULL_PAYMENT_PAID");
            }
        }

        // 2. Thông báo cho Admin chính
        userRepository.findByEmail("admin@hotel.com").ifPresent(admin -> {
            if (hotelOwnerId == null || !admin.getId().equals(hotelOwnerId)) {
                String msg = String.format("Khách hàng đã thanh toán 70%% còn lại cho đơn hàng #%s. Tổng thu: %s ₫.",
                    bookingCode, formatAmount(amount));
                saveNotification(admin, msg, bookingId, bookingCode, "FULL_PAYMENT_PAID");
            }
        });
    }

    /** Gửi thông báo đến chủ khách sạn (ownerId) khi khách thanh toán 70 % */
    @Transactional
    public void notifyOwner(Long ownerId, Long bookingId, String bookingCode, java.math.BigDecimal remainingAmount) {
        if (ownerId == null) return;
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) return;

        String msg = String.format(
                "Khách đã thanh toán 70%% số dư (%s ₫) cho đơn đặt phòng #%s. Đơn hàng hiện đã hoàn tất thanh toán.",
                formatAmount(remainingAmount), bookingCode);

        saveNotification(owner, msg, bookingId, bookingCode, "REMAINING_PAID");
    }

    /** Thông báo cho khách hàng khi vừa đặt phòng xong */
    @Transactional
    public void notifyBookingCreated(com.hotel.backend.model.Booking booking) {
        String msg = String.format("Bạn đã gửi đơn đặt phòng thành công cho khách sạn %s. Mã đơn: #%s. Vui lòng chờ khách sạn xác nhận.",
                booking.getHotel().getHotelName(), booking.getBookingCode());
        sendToUser(booking.getUser(), msg, booking.getId(), booking.getBookingCode(), "BOOKING_CREATED");
    }

    /** Thông báo cho khách hàng khi Admin xác nhận đơn */
    @Transactional
    public void notifyBookingConfirmed(com.hotel.backend.model.Booking booking) {
        String msg = String.format("Cực vui! Đơn đặt phòng #%s của bạn tại %s đã được xác nhận. Chúc bạn có một chuyến đi tuyệt vời!",
                booking.getBookingCode(), booking.getHotel().getHotelName());
        sendToUser(booking.getUser(), msg, booking.getId(), booking.getBookingCode(), "BOOKING_CONFIRMED");
    }

    /** Thông báo cho khách hàng khi đơn bị hủy */
    @Transactional
    public void notifyBookingCancelled(com.hotel.backend.model.Booking booking, String reason) {
        String msg = String.format("Rất tiếc! Đơn đặt phòng #%s của bạn tại %s đã bị hủy. %s",
                booking.getBookingCode(), booking.getHotel().getHotelName(), 
                (reason != null ? "Lý do: " + reason : "Vui lòng liên hệ khách sạn để biết thêm chi tiết."));
        sendToUser(booking.getUser(), msg, booking.getId(), booking.getBookingCode(), "BOOKING_CANCELLED");
    }

    /** Thông báo khi có đánh giá mới */
    @Transactional
    public void notifyNewReview(com.hotel.backend.model.Review review) {
        com.hotel.backend.model.Hotel hotel = review.getHotel();
        if (hotel != null && hotel.getOwner() != null) {
            String msg = String.format("Khách sạn %s vừa có đánh giá %d sao mới từ khách hàng %s.",
                hotel.getHotelName(), review.getRating(), review.getUser().getFullName());
            saveNotification(hotel.getOwner(), msg, null, null, "NEW_REVIEW");
        }
    }

    /** Thông báo khi có tin nhắn chat mới */
    @Transactional
    public void notifyNewChatMessage(com.hotel.backend.model.ChatMessage chatMessage) {
        User receiver = chatMessage.getReceiver();
        if (receiver != null) {
            String contentSnippet = chatMessage.getContent().length() > 30 
                ? chatMessage.getContent().substring(0, 30) + "..." 
                : chatMessage.getContent();
            String msg = String.format("Bạn có tin nhắn mới từ %s: \"%s\"",
                chatMessage.getSender().getFullName(), contentSnippet);
            saveNotification(receiver, msg, null, null, "NEW_CHAT");
        }
    }

    @Transactional
    public void sendToUser(User user, String message, Long bookingId, String bookingCode, String type) {
        saveNotification(user, message, bookingId, bookingCode, type);
    }

    @Transactional
    public void saveNotification(User receiver, String message, Long bookingId, String bookingCode, String type) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .message(message)
                .bookingId(bookingId)
                .bookingCode(bookingCode)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        System.out.println(">>> ĐÃ LƯU THÔNG BÁO CHO: " + receiver.getEmail() + " | Nội dung: " + message);
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
