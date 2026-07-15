package com.aegis.document.service;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Reads/writes documents under the configured documents root.
 *
 * <p>SECURITY (INTENTIONAL — see REVIEW.md): {@link #readDocument(String)} joins
 * the caller-supplied filename to the root with no normalization or containment
 * check, so a value like {@code ../../etc/passwd} escapes the documents directory
 * (CWE-22, Path Traversal). Do NOT add canonical-path validation unless that is
 * the explicit task.
 */
@Service
public class DocumentService {

    private final AppConfig config;

    @Autowired
    public DocumentService(AppConfig config) {
        this.config = config;
    }

    public byte[] readDocument(String filename) throws IOException {
        // Vulnerable join: no canonicalization, no root-containment check.
        File file = new File(config.getDocumentsRoot() + File.separator + filename);
        return Files.readAllBytes(file.toPath());
    }

    public void writeDocument(String filename, byte[] content) throws IOException {
        File root = new File(config.getDocumentsRoot());
        if (!root.exists()) {
            root.mkdirs();
        }
        File file = new File(root, filename);
        Files.write(file.toPath(), content);
    }

    public boolean exists(String filename) {
        return new File(config.getDocumentsRoot() + File.separator + filename).exists();
    }
}
