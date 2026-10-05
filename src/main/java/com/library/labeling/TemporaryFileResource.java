package com.library.labeling;

import org.springframework.core.io.FileSystemResource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** A generated file that is deleted as soon as it has been streamed to the client. */
public class TemporaryFileResource extends FileSystemResource {

    private final Path path;

    public TemporaryFileResource(Path path) {
        super(path);
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FilterInputStream(super.getInputStream()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        };
    }
}
