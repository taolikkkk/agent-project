package cn.hollis.llm.mentor.know.engine.rag.util;

import cn.hollis.llm.mentor.know.engine.chat.constant.RetrievalSource;
import cn.hollis.llm.mentor.know.engine.rag.modules.ContentUtil;
import dev.langchain4j.rag.content.Content;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceUtilTest {

    @Test
    void shouldCreateExplicitReferenceForNeo4jGraphResult() {
        Content graphContent = ContentUtil.markAsGraphResult(Content.from("\"蔚来 ET5 2025 → 搭载 → 空气悬架系统\""));

        var reference = ReferenceUtil.getRagReference(graphContent, RetrievalSource.HYBRID);

        assertThat(reference.getDocumentTitle()).isEqualTo("Neo4j 图谱结果");
        assertThat(reference.getChunkContent()).isEqualTo("\"蔚来 ET5 2025 → 搭载 → 空气悬架系统\"");
        assertThat(reference.getRetrievalSource()).isEqualTo(RetrievalSource.GRAPH_DB);
        assertThat(reference.getMetadata()).containsEntry("provider", "Neo4j");
    }
}
