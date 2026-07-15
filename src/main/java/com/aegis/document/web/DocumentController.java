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
 * <p>SECURITY (INTENTIONAL — see REVIEW.md): the {@code file} parameter is passed
 * straight to {@link DocumentService#readDocument(String)}, which is vulnerable to
 * path traversal (CWE-22). Example: {@code GET /documents/download?file=../../etc/passwd}.
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
    public ResponseEntity<byte[]> download(@RequestParam("file") String file) throws IOException {
        byte[] content = documentService.readDocument(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}
