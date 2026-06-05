package com.hotel.backend.repository;

import com.hotel.backend.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelId(Long hotelId);
}
