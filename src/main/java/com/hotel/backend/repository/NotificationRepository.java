package com.hotel.backend.repository;

import com.hotel.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    void deleteByBookingId(Long bookingId);
}
