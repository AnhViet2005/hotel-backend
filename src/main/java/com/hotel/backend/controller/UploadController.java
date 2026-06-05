package com.hotel.backend.controller;

import com.hotel.backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }
        try {
            String filename = fileUploadService.store(file);
            String fileUrl = "http://localhost:8080/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to upload file: " + e.getMessage()));
        }
    }
}
