package com.hotel.backend.controller;

import com.hotel.backend.model.ContactInfo;
import com.hotel.backend.repository.ContactInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/contact-info")
public class ContactInfoController {

    @Autowired
    private ContactInfoRepository contactInfoRepository;

    @GetMapping
    public ResponseEntity<?> getContactInfo() {
        Optional<ContactInfo> contactInfo = contactInfoRepository.findFirstByOrderByIdDesc();
        if (contactInfo.isPresent()) {
            return ResponseEntity.ok(contactInfo.get());
        }
        return ResponseEntity.notFound().build();
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createContactInfo(@RequestBody ContactInfo contactInfo) {
        ContactInfo saved = contactInfoRepository.save(contactInfo);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateContactInfo(@PathVariable Long id, @RequestBody ContactInfo contactInfo) {
        Optional<ContactInfo> existing = contactInfoRepository.findById(id);
        if (existing.isPresent()) {
            ContactInfo updated = existing.get();
            updated.setEmail(contactInfo.getEmail());
            updated.setPhone(contactInfo.getPhone());
            updated.setAddress(contactInfo.getAddress());
            updated.setCompanyName(contactInfo.getCompanyName());
            updated.setWebsiteUrl(contactInfo.getWebsiteUrl());
            updated.setFacebookUrl(contactInfo.getFacebookUrl());
            updated.setInstagramUrl(contactInfo.getInstagramUrl());
            updated.setTwitterUrl(contactInfo.getTwitterUrl());
            updated.setSiteName(contactInfo.getSiteName());
            updated.setSiteDescription(contactInfo.getSiteDescription());
            updated.setSeoKeywords(contactInfo.getSeoKeywords());
            updated.setCommissionRate(contactInfo.getCommissionRate());
            
            ContactInfo result = contactInfoRepository.save(updated);
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
}
