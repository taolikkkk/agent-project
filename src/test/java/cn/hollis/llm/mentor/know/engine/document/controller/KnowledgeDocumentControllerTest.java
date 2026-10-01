package cn.hollis.llm.mentor.know.engine.document.controller;

import cn.hollis.llm.mentor.know.engine.document.entity.KnowledgeDocumentVersion;
import cn.hollis.llm.mentor.know.engine.document.service.FileStorageService;
import cn.hollis.llm.mentor.know.engine.document.service.KnowledgeDocumentVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeDocumentControllerTest {

    @Test
    void originalFileRedirectsToSignedMinioUrl() throws Exception {
        KnowledgeDocumentVersionService versions = mock(KnowledgeDocumentVersionService.class);
        FileStorageService storage = mock(FileStorageService.class);
        KnowledgeDocumentVersion version = new KnowledgeDocumentVersion();
        version.setDocUrl("http://localhost:9000/know-engine/original/guide.md");
        when(versions.getById(7L)).thenReturn(version);
        when(storage.getDownloadUrl(version.getDocUrl())).thenReturn("http://localhost:9000/know-engine/original/guide.md?X-Amz-Signature=test");

        KnowledgeDocumentController controller = new KnowledgeDocumentController();
        ReflectionTestUtils.setField(controller, "knowledgeDocumentVersionService", versions);
        ReflectionTestUtils.setField(controller, "fileStorageService", storage);

        ResponseEntity<Void> response = controller.openOriginal(7L);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create("http://localhost:9000/know-engine/original/guide.md?X-Amz-Signature=test"), response.getHeaders().getLocation());
    }
}
