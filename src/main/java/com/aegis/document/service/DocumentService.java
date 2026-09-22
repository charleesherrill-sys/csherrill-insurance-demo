package com.aegis.document.service;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Reads and writes documents under the configured documents root. */
@Service
public class DocumentService {

    private final AppConfig config;

    @Autowired
    public DocumentService(AppConfig config) {
        this.config = config;
    }

    public byte[] readDocument(String filename) throws IOException {
        return Files.readAllBytes(resolveWithinRoot(filename));
    }

    public void writeDocument(String filename, byte[] content) throws IOException {
        Path root = Paths.get(config.getDocumentsRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.write(resolveWithinRoot(filename), content);
    }

    public boolean exists(String filename) {
        try {
            return Files.exists(resolveWithinRoot(filename));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Path resolveWithinRoot(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid document path");
        }
        Path root = Paths.get(config.getDocumentsRoot()).toAbsolutePath().normalize();
        Path resolved = root.resolve(filename).normalize();
        if (resolved.equals(root) || !resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid document path");
        }
        return resolved;
    }
}
