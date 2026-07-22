package com.aegis.document.service;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads/writes documents under the configured documents root.
 *
 * <p>Caller-supplied filenames are resolved against the documents root and the
 * resolved path is validated to stay inside that root before any filesystem
 * access, preventing path traversal (CWE-22).
 */
@Service
public class DocumentService {

    private final AppConfig config;

    @Autowired
    public DocumentService(AppConfig config) {
        this.config = config;
    }

    public byte[] readDocument(String filename) throws IOException {
        Path file = resolveWithinRoot(filename);
        return Files.readAllBytes(file);
    }

    public void writeDocument(String filename, byte[] content) throws IOException {
        Path file = resolveWithinRoot(filename);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
    }

    public boolean exists(String filename) {
        try {
            return Files.exists(resolveWithinRoot(filename));
        } catch (IOException e) {
            return false;
        }
    }

    private Path documentsRoot() {
        return Paths.get(config.getDocumentsRoot()).toAbsolutePath().normalize();
    }

    /**
     * Resolves {@code filename} against the documents root and enforces that the
     * result stays inside the root. Rejects absolute paths and {@code ..}
     * traversal sequences that would escape the documents directory.
     */
    private Path resolveWithinRoot(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("Invalid document path: filename is empty");
        }
        Path base = documentsRoot();
        Path resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base)) {
            throw new IOException("Invalid document path: " + filename);
        }
        return resolved;
    }
}
