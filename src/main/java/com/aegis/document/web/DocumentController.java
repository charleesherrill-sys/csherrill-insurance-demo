package com.aegis.document.web;

import com.aegis.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Document download endpoint.
 *
 * <p>The {@code file} parameter is resolved and containment-checked against the
 * documents root by {@link DocumentService#readDocument(String)}; requests that
 * escape the root (path traversal, CWE-22) are rejected with a 404.
 */
@Controller
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents/download")
    @ResponseBody
    public ResponseEntity<byte[]> download(@RequestParam("file") String file) {
        byte[] content;
        try {
            content = documentService.readDocument(file);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizeHeader(file) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    private static String sanitizeHeader(String filename) {
        return filename == null ? "" : filename.replaceAll("[\\r\\n\"\\\\/]", "_");
    }
}
