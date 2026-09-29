package cn.hollis.llm.mentor.know.engine.document.job;

import cn.hollis.llm.mentor.know.engine.document.constant.DocumentStatus;
import cn.hollis.llm.mentor.know.engine.document.entity.KnowledgeDocument;
import cn.hollis.llm.mentor.know.engine.document.entity.KnowledgeDocumentVersion;
import cn.hollis.llm.mentor.know.engine.document.service.DocumentProcessService;
import cn.hollis.llm.mentor.know.engine.document.service.KnowledgeDocumentService;
import cn.hollis.llm.mentor.know.engine.document.service.KnowledgeDocumentVersionService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentCompensationJobTest {

    @Test
    void successfulCompensationAdvancesCurrentDocumentStatus() {
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeDocumentVersionService versionService = mock(KnowledgeDocumentVersionService.class);
        DocumentProcessService processService = mock(DocumentProcessService.class);
        DocumentCompensationJob job = job(documentService, versionService, processService);

        KnowledgeDocumentVersion version = version(10L, 7L);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setCurrentVersionId(7L);
        when(versionService.list(org.mockito.Mockito.<Wrapper<KnowledgeDocumentVersion>>any())).thenReturn(List.of(version));
        when(documentService.getById(10L)).thenReturn(document);
        when(processService.embedAndStore(version)).thenReturn(true);

        job.documentEmbeddingCompensation();

        verify(documentService).advanceDocumentAndVersionStatus(10L, 7L, DocumentStatus.VECTOR_STORED);
    }

    @Test
    void compensationDoesNotAdvanceHistoricalVersion() {
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        KnowledgeDocumentVersionService versionService = mock(KnowledgeDocumentVersionService.class);
        DocumentProcessService processService = mock(DocumentProcessService.class);
        DocumentCompensationJob job = job(documentService, versionService, processService);

        KnowledgeDocumentVersion version = version(10L, 6L);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setCurrentVersionId(7L);
        when(versionService.list(org.mockito.Mockito.<Wrapper<KnowledgeDocumentVersion>>any())).thenReturn(List.of(version));
        when(documentService.getById(10L)).thenReturn(document);

        job.documentEmbeddingCompensation();

        verifyNoInteractions(processService);
        verify(documentService, never()).advanceDocumentAndVersionStatus(any(), any(), any());
    }

    private DocumentCompensationJob job(KnowledgeDocumentService documentService,
                                        KnowledgeDocumentVersionService versionService,
                                        DocumentProcessService processService) {
        DocumentCompensationJob job = new DocumentCompensationJob();
        ReflectionTestUtils.setField(job, "knowledgeDocumentService", documentService);
        ReflectionTestUtils.setField(job, "knowledgeDocumentVersionService", versionService);
        ReflectionTestUtils.setField(job, "documentProcessService", processService);
        return job;
    }

    private KnowledgeDocumentVersion version(Long docId, Long versionId) {
        KnowledgeDocumentVersion version = new KnowledgeDocumentVersion();
        version.setDocId(docId);
        version.setVersionId(versionId);
        version.setVersion("1.0.0");
        version.setStatus(DocumentStatus.CHUNKED);
        return version;
    }
}
