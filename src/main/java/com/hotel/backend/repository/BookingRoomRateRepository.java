package com.hotel.backend.repository;

import com.hotel.backend.model.BookingRoomRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRoomRateRepository extends JpaRepository<BookingRoomRate, Long> {
}
