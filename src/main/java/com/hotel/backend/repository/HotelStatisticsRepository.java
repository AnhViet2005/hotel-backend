package com.hotel.backend.repository;

import com.hotel.backend.model.HotelStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HotelStatisticsRepository extends JpaRepository<HotelStatistics, Long> {
    List<HotelStatistics> findByHotelId(Long hotelId);
}
