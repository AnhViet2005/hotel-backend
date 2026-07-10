package com.hotel.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Uploads files to Cloudinary cloud storage.
 * Returns a permanent public URL (https://res.cloudinary.com/...).
 * No local filesystem used — safe for Railway/Vercel ephemeral containers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file to Cloudinary and returns the secure public URL.
     * The URL is permanent and does NOT depend on the server's filesystem.
     *
     * @param file multipart file from request
     * @return full Cloudinary URL e.g. https://res.cloudinary.com/demo/image/upload/xxx.jpg
     */
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot upload empty file.");
            }

            String publicId = "hotel-uploads/" + UUID.randomUUID();

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "auto",
                "overwrite", false
            ));

            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Kept for backward-compat. Returns the URL directly (same as store()).
     */
    public String storeAndGetUrl(MultipartFile file) {
        return store(file);
    }
}
