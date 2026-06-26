package com.library.service;

import com.library.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalDiskStorageService implements StorageService {

    @Value("${storage.base-path:/data/library-files}")
    private String basePath;

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("فایل خالی است");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID() + extension;
        String relativePath = subDirectory + "/" + storedFilename;

        try {
            Path targetDir = Paths.get(basePath, subDirectory);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage());
            throw new RuntimeException("ذخیره‌ی فایل ناموفق بود", e);
        }
    }

    @Override
    public Resource load(String storedPath) {
        try {
            Path file = Paths.get(basePath).resolve(storedPath);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("فایل پیدا نشد یا قابل خواندن نیست: " + storedPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("بارگذاری فایل ناموفق بود: " + storedPath, e);
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            Path file = Paths.get(basePath).resolve(storedPath);
            Files.deleteIfExists(file);
            log.info("Deleted file: {}", storedPath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storedPath);
        }
    }
}
