package com.library.repository;

import com.library.entity.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileResourceRepository extends JpaRepository<FileResource, Long> {
    Optional<FileResource> findByChecksumSha256(String checksum);
}
