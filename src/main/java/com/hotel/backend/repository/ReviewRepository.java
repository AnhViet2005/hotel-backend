package com.hotel.backend.repository;

import com.hotel.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHotelId(Long hotelId);
    List<Review> findByHotelIdAndIsPublishedTrue(Long hotelId);
}
