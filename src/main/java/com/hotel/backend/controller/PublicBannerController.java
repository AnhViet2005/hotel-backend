package com.hotel.backend.controller;

import com.hotel.backend.model.Banner;
import com.hotel.backend.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/banners")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicBannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerRepository.findByIsActiveOrderByDisplayOrderAsc(true));
    }
}
