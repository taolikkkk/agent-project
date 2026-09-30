package cn.hollis.llm.mentor.know.engine.rag.util;

import cn.hollis.llm.mentor.know.engine.chat.constant.RetrievalSource;
import cn.hollis.llm.mentor.know.engine.chat.entity.ChatMessage;
import cn.hollis.llm.mentor.know.engine.rag.modules.ContentUtil;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.hollis.llm.mentor.know.engine.rag.constant.MetadataKeyConstant.*;

public class ReferenceUtil {

    private ReferenceUtil() {
    }

    public static List<ChatMessage.RagReference> getRagReferences(List<Content> contents, RetrievalSource retrievalSource) {
        return contents.stream()
                .map(content -> getRagReference(content, retrievalSource))
                .collect(Collectors.toList());
    }

    public static ChatMessage.RagReference getRagReference(Content content, RetrievalSource retrievalSource) {
        if (ContentUtil.isGraphResult(content)) {
            return ChatMessage.RagReference.builder()
                    .documentTitle("Neo4j 图谱结果")
                    .chunkContent(content.textSegment().text())
                    .retrievalSource(RetrievalSource.GRAPH_DB)
                    .metadata(Map.of("provider", "Neo4j", "kind", "图关系查询"))
                    .build();
        }

        var metadata = content.textSegment().metadata();
        return ChatMessage.RagReference.builder()
                .documentId(Objects.toString(metadata.getInteger(DOC_ID), null))
                .documentTitle(metadata.getString(FILE_NAME))
                .url(metadata.getString(URL))
                .chunkId(metadata.getString(CHUNK_ID))
                .embeddingId(metadata.getString(EMBEDDING_ID))
                .version(Objects.toString(metadata.getInteger(VERSION), null))
                .chunkContent(content.textSegment().text())
                .similarityScore((Double) content.metadata().get(ContentMetadata.SCORE))
                .retrievalSource(retrievalSource)
                .rerankScore((Double) content.metadata().get(ContentMetadata.RERANKED_SCORE))
                .build();
    }
}
