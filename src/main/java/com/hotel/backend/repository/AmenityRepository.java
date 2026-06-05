package com.hotel.backend.repository;

import com.hotel.backend.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    java.util.Optional<Amenity> findByAmenityName(String amenityName);
}
