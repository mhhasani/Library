package com.library.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Store a file and return the stored relative path (used as key).
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Load a file as a streamable Resource.
     */
    Resource load(String storedPath);

    /**
     * Delete a file by its stored path.
     */
    void delete(String storedPath);
}
