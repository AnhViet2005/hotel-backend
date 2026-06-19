package com.hotel.backend.repository;

import com.hotel.backend.model.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomTypeId(Long roomTypeId);
    
    @org.springframework.data.jpa.repository.Query("SELECT ri FROM RoomImage ri WHERE ri.roomType.id = :roomTypeId AND ri.isPrimary = true")
    java.util.Optional<RoomImage> findPrimaryByRoomTypeId(Long roomTypeId);
}
