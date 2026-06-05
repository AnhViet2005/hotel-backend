package com.hotel.backend.repository;

import com.hotel.backend.model.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    List<BookingRoom> findByBookingId(Long bookingId);
}
