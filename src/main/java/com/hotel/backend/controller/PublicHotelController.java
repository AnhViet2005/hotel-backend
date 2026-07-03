package com.hotel.backend.controller;

import com.hotel.backend.dto.PublicHotelResponse;
import com.hotel.backend.service.PublicHotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/hotels")
@RequiredArgsConstructor
public class PublicHotelController {

    private final PublicHotelService publicHotelService;

    @GetMapping
    public ResponseEntity<List<PublicHotelResponse>> getAllHotels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut) {
        return ResponseEntity.ok(publicHotelService.getAllActiveHotels(keyword, checkIn, checkOut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicHotelResponse> getHotelById(
            @PathVariable Long id,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut) {
        return ResponseEntity.ok(publicHotelService.getHotelById(id, checkIn, checkOut));
    }
}
