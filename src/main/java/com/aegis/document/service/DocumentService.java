package com.aegis.document.service;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads/writes documents under the configured documents root.
 *
 * <p>SECURITY (CWE-22): {@link #resolveWithinRoot(String)} normalizes the
 * caller-supplied filename against the documents root and verifies that the
 * resolved canonical path stays inside the root directory. Path-traversal
 * attempts ({@code ../}), absolute paths, and other escapes are rejected before
 * any filesystem access.
 */
@Service
public class DocumentService {

    private final AppConfig config;

    @Autowired
    public DocumentService(AppConfig config) {
        this.config = config;
    }

    public byte[] readDocument(String filename) throws IOException {
        File file = resolveWithinRoot(filename);
        return Files.readAllBytes(file.toPath());
    }

    public void writeDocument(String filename, byte[] content) throws IOException {
        File root = new File(config.getDocumentsRoot());
        if (!root.exists()) {
            root.mkdirs();
        }
        File file = resolveWithinRoot(filename);
        Files.write(file.toPath(), content);
    }

    public boolean exists(String filename) {
        try {
            return resolveWithinRoot(filename).exists();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Resolves {@code filename} against the documents root and guarantees the
     * result is contained within that root. Rejects null/blank names, absolute
     * paths, and any traversal that escapes the root.
     *
     * @throws IOException if the name is invalid or resolves outside the root
     */
    private File resolveWithinRoot(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("invalid document name");
        }
        Path candidate = Paths.get(filename);
        if (candidate.isAbsolute()) {
            throw new IOException("absolute document paths are not allowed: " + filename);
        }
        Path root = Paths.get(config.getDocumentsRoot()).toAbsolutePath().normalize();
        Path resolved = root.resolve(candidate).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("document path escapes storage root: " + filename);
        }
        return resolved.toFile();
    }
}
