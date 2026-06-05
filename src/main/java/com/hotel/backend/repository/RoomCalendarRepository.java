package com.hotel.backend.repository;

import com.hotel.backend.model.RoomCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RoomCalendarRepository extends JpaRepository<RoomCalendar, Long> {
    List<RoomCalendar> findByRoomTypeIdAndDateBetween(Long roomTypeId, LocalDate startDate, LocalDate endDate);
}
