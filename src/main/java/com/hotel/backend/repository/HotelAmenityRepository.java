package com.hotel.backend.repository;

import com.hotel.backend.model.HotelAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, HotelAmenity.HotelAmenityId> {
}
