package com.aegis.document;

import com.aegis.common.config.AppConfig;
import com.aegis.document.service.DocumentService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Path-traversal containment on the document service (CWE-22 regression). */
public class DocumentServiceTest {

    private Path root;
    private DocumentService service;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("aegis-docs");
        AppConfig config = mock(AppConfig.class);
        when(config.getDocumentsRoot()).thenReturn(root.toString());
        service = new DocumentService(config);
    }

    @Test
    public void readsFileInsideRoot() throws IOException {
        Files.write(root.resolve("eob.pdf"), "hello".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), service.readDocument("eob.pdf"));
    }

    @Test
    public void rejectsRelativeTraversal() throws IOException {
        Path secret = root.getParent().resolve("secret-" + root.getFileName() + ".txt");
        Files.write(secret, "top-secret".getBytes(StandardCharsets.UTF_8));
        try {
            service.readDocument("../" + secret.getFileName());
            fail("expected traversal to be rejected");
        } catch (IOException expected) {
            // path escapes the documents root
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    public void rejectsAbsolutePathEscape() {
        try {
            service.readDocument("/etc/passwd");
            fail("expected absolute path escape to be rejected");
        } catch (IOException expected) {
            // absolute path resolves outside the documents root
        }
    }

    @Test
    public void existsReturnsFalseForTraversal() {
        assertFalse(service.exists("../../etc/passwd"));
    }

    @Test
    public void writeStaysInsideRoot() throws IOException {
        service.writeDocument("nested/report.txt", "data".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(root.resolve("nested/report.txt")));
    }

    @Test
    public void writeRejectsTraversal() {
        try {
            service.writeDocument("../escape.txt", "data".getBytes(StandardCharsets.UTF_8));
            fail("expected write traversal to be rejected");
        } catch (IOException expected) {
            // write target escapes the documents root
        }
    }
}
