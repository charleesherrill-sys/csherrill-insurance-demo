package com.aegis.document;

import com.aegis.common.config.AppConfig;
import com.aegis.document.service.DocumentService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writesAndReadsDocumentWithinRoot() throws Exception {
        DocumentService service = service();
        byte[] content = "explanation of benefits".getBytes(StandardCharsets.UTF_8);

        service.writeDocument("eob-90233.pdf", content);

        assertTrue(service.exists("eob-90233.pdf"));
        assertArrayEquals(content, service.readDocument("eob-90233.pdf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTraversalOnRead() throws Exception {
        service().readDocument("../../etc/passwd");
    }

    @Test
    public void traversalDoesNotAppearToExist() {
        assertFalse(service().exists("../../etc/passwd"));
    }

    @Test
    public void rejectsTraversalOnWrite() throws Exception {
        try {
            service().writeDocument("../escape-aegis-test.txt", new byte[] {1});
        } catch (IllegalArgumentException expected) {
            File escaped = new File(folder.getRoot().getParentFile(), "escape-aegis-test.txt");
            assertFalse(escaped.exists());
            return;
        }
        throw new AssertionError("expected invalid document path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAbsolutePath() throws Exception {
        service().readDocument("/etc/passwd");
    }

    private DocumentService service() {
        AppConfig config = new AppConfig();
        ReflectionTestUtils.setField(config, "documentsRoot", folder.getRoot().getAbsolutePath());
        return new DocumentService(config);
    }
}
