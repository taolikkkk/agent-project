package cn.hollis.llm.mentor.know.engine.document.service;

import cn.hollis.llm.mentor.know.engine.document.entity.DocumentUploadParam;
import cn.hollis.llm.mentor.know.engine.document.service.impl.DocumentProcessServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentProcessServiceImplTest {

    @Test
    void shouldRejectCsvForDocumentSearch() {
        DocumentProcessServiceImpl service = new DocumentProcessServiceImpl();
        MockMultipartFile csv = new MockMultipartFile("file", "service_price.csv", "text/csv", "项目,价格".getBytes());
        DocumentUploadParam param = new DocumentUploadParam(
                csv, "服务价格", "VISITOR", "服务价格表", "DOCUMENT_SEARCH", null, "1.0.0");

        assertThrows(IllegalArgumentException.class, () -> service.upload(param, "测试员工"));
    }
}
