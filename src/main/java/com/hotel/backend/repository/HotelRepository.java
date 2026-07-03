package com.hotel.backend.repository;

import com.hotel.backend.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByCity(String city);
    List<Hotel> findByIsActiveTrueAndIsApprovedTrue();
    List<Hotel> findByIsActiveTrue();

    @org.springframework.data.jpa.repository.Query("SELECT h FROM Hotel h WHERE h.isActive = true AND h.isApproved = true AND (" +
            "LOWER(REPLACE(REPLACE(h.hotelName, ' ', ''), '.', '')) LIKE LOWER(REPLACE(REPLACE(CONCAT('%', :keyword, '%'), ' ', ''), '.', '')) OR " +
            "LOWER(REPLACE(REPLACE(h.city, ' ', ''), '.', '')) LIKE LOWER(REPLACE(REPLACE(CONCAT('%', :keyword, '%'), ' ', ''), '.', '')) OR " +
            "LOWER(REPLACE(REPLACE(coalesce(h.district, ''), ' ', ''), '.', '')) LIKE LOWER(REPLACE(REPLACE(CONCAT('%', :keyword, '%'), ' ', ''), '.', '')) OR " +
            "LOWER(REPLACE(REPLACE(coalesce(h.addressLine, ''), ' ', ''), '.', '')) LIKE LOWER(REPLACE(REPLACE(CONCAT('%', :keyword, '%'), ' ', ''), '.', ''))" +
            ")")
    List<Hotel> searchHotels(String keyword);
}
