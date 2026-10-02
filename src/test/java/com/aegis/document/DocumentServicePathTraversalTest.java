package com.aegis.document;

import com.aegis.common.config.AppConfig;
import com.aegis.document.service.DocumentService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;

/**
 * Regression tests for CWE-22 path traversal fix in DocumentService.
 */
public class DocumentServicePathTraversalTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private DocumentService serviceWithRoot(String root) {
        AppConfig cfg = new AppConfig() {
            @Override
            public String getDocumentsRoot() {
                return root;
            }
        };
        return new DocumentService(cfg);
    }

    @Test(expected = SecurityException.class)
    public void readDocument_rejectsDotDotTraversal() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        svc.readDocument("../../etc/passwd");
    }

    @Test(expected = SecurityException.class)
    public void readDocument_rejectsAbsoluteSlash() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        svc.readDocument("/etc/passwd");
    }

    @Test(expected = SecurityException.class)
    public void writeDocument_rejectsDotDotTraversal() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        svc.writeDocument("../escape.txt", "data".getBytes());
    }

    @Test(expected = SecurityException.class)
    public void writeDocument_rejectsSlashInFilename() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        svc.writeDocument("sub/dir/file.txt", "data".getBytes());
    }

    @Test
    public void exists_returnsFalseForTraversalAttempt() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        assertFalse(svc.exists("../../etc/passwd"));
    }

    @Test
    public void readDocument_allowsLegitimateFilename() throws IOException {
        File root = tmp.getRoot();
        byte[] content = "hello".getBytes();
        java.nio.file.Files.write(new File(root, "test.pdf").toPath(), content);
        DocumentService svc = serviceWithRoot(root.getAbsolutePath());
        assertArrayEquals(content, svc.readDocument("test.pdf"));
    }

    @Test
    public void writeDocument_allowsLegitimateFilename() throws IOException {
        DocumentService svc = serviceWithRoot(tmp.getRoot().getAbsolutePath());
        byte[] data = "content".getBytes();
        svc.writeDocument("output.pdf", data);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(
                new File(tmp.getRoot(), "output.pdf").toPath()));
    }
}
