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
import java.nio.file.Paths;

/** Document download endpoint. */
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
        try {
            byte[] content = documentService.readDocument(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + Paths.get(file).getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
