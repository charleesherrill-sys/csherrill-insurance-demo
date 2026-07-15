package com.aegis.document;

import com.aegis.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Writes the sample documents referenced by the seed data into the documents root
 * on startup, so the download endpoint has real files to serve during the demo.
 */
@Component
public class DocumentSeeder implements ApplicationRunner {

    private final DocumentService documentService;

    @Autowired
    public DocumentSeeder(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seed("eob-90233.pdf", "Explanation of Benefits for claim CLM-90233 (Alex Morgan).\n");
        seed("referral-90311.pdf", "Referral document for claim CLM-90311 (Bailey Hopkins).\n");
    }

    private void seed(String filename, String body) throws Exception {
        if (!documentService.exists(filename)) {
            documentService.writeDocument(filename, body.getBytes(StandardCharsets.UTF_8));
        }
    }
}
