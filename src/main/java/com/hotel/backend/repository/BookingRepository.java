package com.hotel.backend.repository;

import com.hotel.backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
