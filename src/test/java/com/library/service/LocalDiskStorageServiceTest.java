package com.library.service;

import com.library.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Local Disk Storage Service Tests")
class LocalDiskStorageServiceTest {

    private LocalDiskStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalDiskStorageService();
        ReflectionTestUtils.setField(storageService, "basePath", tempDir.toString());
    }

    @Test
    @DisplayName("store() writes the file to disk under the given subdirectory and returns its relative path")
    void store_writesFileAndReturnsRelativePath() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "pdf content".getBytes());

        String relativePath = storageService.store(file, "digital-books");

        assertThat(relativePath).startsWith("digital-books/").endsWith(".pdf");
        Path stored = tempDir.resolve(relativePath);
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readString(stored)).isEqualTo("pdf content");
    }

    @Test
    @DisplayName("store() generates a unique filename, preserving the original extension")
    void store_generatesUniqueFilenamePerCall() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "img".getBytes());

        String path1 = storageService.store(file, "covers");
        String path2 = storageService.store(file, "covers");

        assertThat(path1).isNotEqualTo(path2);
        assertThat(path1).endsWith(".png");
        assertThat(path2).endsWith(".png");
    }

    @Test
    @DisplayName("store() rejects an empty file")
    void store_emptyFile_throwsBadRequest() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storageService.store(empty, "digital-books"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فایل خالی است");
    }

    @Test
    @DisplayName("store() rejects a null file")
    void store_nullFile_throwsBadRequest() {
        assertThatThrownBy(() -> storageService.store(null, "digital-books"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("load() returns a readable resource for a previously stored file")
    void load_existingFile_returnsReadableResource() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "hello world".getBytes());
        String relativePath = storageService.store(file, "digital-books");

        Resource resource = storageService.load(relativePath);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("load() throws for a nonexistent path")
    void load_missingFile_throws() {
        assertThatThrownBy(() -> storageService.load("digital-books/does-not-exist.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("فایل پیدا نشد");
    }

    @Test
    @DisplayName("delete() removes a stored file")
    void delete_existingFile_removesIt() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "content".getBytes());
        String relativePath = storageService.store(file, "digital-books");
        Path stored = tempDir.resolve(relativePath);
        assertThat(Files.exists(stored)).isTrue();

        storageService.delete(relativePath);

        assertThat(Files.exists(stored)).isFalse();
    }

    @Test
    @DisplayName("delete() on a nonexistent path does not throw")
    void delete_missingFile_doesNotThrow() {
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> storageService.delete("digital-books/does-not-exist.pdf"));
    }

    @Test
    @DisplayName("store() wraps an IOException as a RuntimeException")
    void store_ioFailure_wrapsAsRuntimeException() throws IOException {
        // basePath pointing at a regular file (not a directory) makes Files.createDirectories fail with an IOException.
        Path notADirectory = tempDir.resolve("not-a-directory");
        Files.writeString(notADirectory, "blocking file");
        ReflectionTestUtils.setField(storageService, "basePath", notADirectory.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> storageService.store(file, "digital-books"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ذخیره‌ی فایل ناموفق بود");
    }

    @Test
    @DisplayName("delete() swallows an IOException (e.g. a non-empty directory) and logs a warning instead of throwing")
    void delete_ioFailure_doesNotThrow() throws IOException {
        // A non-empty directory at the "file" path makes Files.deleteIfExists throw DirectoryNotEmptyException (an IOException).
        Path dirActingAsFile = tempDir.resolve("digital-books/not-empty-dir");
        Files.createDirectories(dirActingAsFile);
        Files.writeString(dirActingAsFile.resolve("child.txt"), "content");

        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> storageService.delete("digital-books/not-empty-dir"));
        assertThat(Files.exists(dirActingAsFile)).isTrue();
    }
}
