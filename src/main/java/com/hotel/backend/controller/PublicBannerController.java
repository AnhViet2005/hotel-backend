package com.hotel.backend.controller;

import com.hotel.backend.model.Banner;
import com.hotel.backend.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/banners")
@RequiredArgsConstructor
public class PublicBannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerRepository.findByIsActiveOrderByDisplayOrderAsc(true));
    }

    @Autowired
    private org.springframework.core.env.Environment env;

    @GetMapping("/db")
    public ResponseEntity<?> getDbInfo() {
        java.util.Map<String, String> info = new java.util.HashMap<>();
        info.put("DATABASE_URL", System.getenv("DATABASE_URL"));
        info.put("SPRING_DATASOURCE_URL", env.getProperty("spring.datasource.url"));
        info.put("USERNAME", env.getProperty("spring.datasource.username"));
        info.put("PASSWORD", env.getProperty("spring.datasource.password"));
        return ResponseEntity.ok(info);
    }
}
