package com.aegis.document.service;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Reads/writes documents under the configured documents root.
 */
@Service
public class DocumentService {

    private final AppConfig config;

    @Autowired
    public DocumentService(AppConfig config) {
        this.config = config;
    }

    /**
     * Validates that the resolved path stays within the documents root.
     * Rejects filenames containing path separators or '..' components.
     *
     * @throws SecurityException if the filename escapes the documents root
     */
    private File safeResolve(String filename) throws IOException {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("Invalid document filename: " + filename);
        }
        File root = new File(config.getDocumentsRoot()).getCanonicalFile();
        File resolved = new File(root, filename).getCanonicalFile();
        if (!resolved.getPath().startsWith(root.getPath() + File.separator)
                && !resolved.equals(root)) {
            throw new SecurityException("Document path escapes root: " + filename);
        }
        return resolved;
    }

    public byte[] readDocument(String filename) throws IOException {
        File file = safeResolve(filename);
        return Files.readAllBytes(file.toPath());
    }

    public void writeDocument(String filename, byte[] content) throws IOException {
        File root = new File(config.getDocumentsRoot()).getCanonicalFile();
        if (!root.exists()) {
            root.mkdirs();
        }
        File file = safeResolve(filename);
        Files.write(file.toPath(), content);
    }

    public boolean exists(String filename) {
        try {
            return safeResolve(filename).exists();
        } catch (IOException | SecurityException e) {
            return false;
        }
    }
}
