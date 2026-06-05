package com.hotel.backend.repository;

import com.hotel.backend.model.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomTypeId(Long roomTypeId);
}
