package com.hotel.backend.controller;

import com.hotel.backend.dto.AdminHotelRequest;
import com.hotel.backend.dto.AdminHotelResponse;
import com.hotel.backend.service.AdminHotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
public class AdminHotelController {

    private final AdminHotelService adminHotelService;

    @GetMapping
    public ResponseEntity<List<AdminHotelResponse>> getAll(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminHotelService.getAll(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminHotelResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminHotelService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AdminHotelResponse> create(@RequestBody AdminHotelRequest request) {
        return ResponseEntity.ok(adminHotelService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminHotelResponse> update(@PathVariable Long id,
                                                     @RequestBody AdminHotelRequest request) {
        return ResponseEntity.ok(adminHotelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam(defaultValue = "soft") String type) {
        if ("hard".equals(type)) {
            adminHotelService.hardDelete(id);
        } else {
            adminHotelService.delete(id);
        }
        return ResponseEntity.noContent().build();
    }
}
