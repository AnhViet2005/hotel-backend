package com.hotel.backend.repository;

import com.hotel.backend.model.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findByHotelId(Long hotelId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM HotelImage i WHERE i.hotel.id = :hotelId AND i.isPrimary = true")
    Optional<HotelImage> findPrimaryByHotelId(Long hotelId);
}
