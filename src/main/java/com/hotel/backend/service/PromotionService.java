package com.hotel.backend.service;

import com.hotel.backend.model.Promotion;
import com.hotel.backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Kiểm tra mã giảm giá và tính toán số tiền được giảm.
     * Trả về số tiền được giảm (discountAmount).
     */
    public BigDecimal calculateDiscount(String promoCode, Long hotelId, BigDecimal orderTotal) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Promotion promo = promotionRepository.findByPromoCodeAndHotelId(promoCode.trim(), hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không hợp lệ cho khách sạn này."));

        // 1. Kiểm tra trạng thái hoạt động
        if (promo.getIsActive() == null || !promo.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá hiện không khả dụng.");
        }

        // 2. Kiểm tra thời hạn
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promo.getStartDate()) || now.isAfter(promo.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn hoặc chưa đến thời gian áp dụng.");
        }

        // 3. Kiểm tra giá trị đơn hàng tối thiểu
        if (promo.getMinOrderValue() != null && orderTotal.compareTo(promo.getMinOrderValue()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                String.format("Đơn hàng phải có giá trị tối thiểu %,.0f ₫ để áp dụng mã này.", promo.getMinOrderValue()));
        }

        // 4. Tính toán số tiền giảm
        BigDecimal discountAmount = orderTotal.multiply(promo.getDiscountPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        // 5. Kiểm tra giới hạn giảm tối đa
        if (promo.getMaxDiscountAmount() != null && discountAmount.compareTo(promo.getMaxDiscountAmount()) > 0) {
            discountAmount = promo.getMaxDiscountAmount();
        }

        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }
}
