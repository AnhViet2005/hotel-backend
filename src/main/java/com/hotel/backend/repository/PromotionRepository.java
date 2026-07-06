package com.hotel.backend.repository;

import com.hotel.backend.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByPromoCode(String promoCode);
    Optional<Promotion> findByPromoCodeAndHotelId(String promoCode, Long hotelId);
}
