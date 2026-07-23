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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

/**
 * Document download endpoint.
 *
 * <p>SECURITY (CWE-22): the {@code file} parameter is validated by
 * {@link DocumentService#readDocument(String)}, which rejects any name that
 * escapes the storage root. Rejected traversal attempts return {@code 400 Bad
 * Request}; missing files return {@code 404 Not Found}. The download filename in
 * the response header is reduced to its base name so the caller-supplied path
 * cannot be reflected verbatim.
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
        } catch (NoSuchFileException | FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            // Containment/validation failure (e.g. path traversal attempt).
            return ResponseEntity.badRequest().build();
        }
        String downloadName = Paths.get(file).getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}
