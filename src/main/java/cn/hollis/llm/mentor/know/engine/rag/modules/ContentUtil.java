package cn.hollis.llm.mentor.know.engine.rag.modules;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;

import static cn.hollis.llm.mentor.know.engine.rag.constant.MetadataKeyConstant.GRAPH_RESULT;
import static cn.hollis.llm.mentor.know.engine.rag.constant.MetadataKeyConstant.SKIP_RERANK;

/**
 * RAG 内容工具类
 */
public class ContentUtil {

    private ContentUtil() {
    }

    /**
     * 将内容标记为跳过重排序/融合。
     *
     * @param content 原始内容
     * @return 带有 skipRerank 标记的内容
     */
    public static Content markAsSkipRerank(Content content) {
        return withMetadata(content, SKIP_RERANK, "true");
    }

    /**
     * 标记 Neo4j 查询结果，使其保留图数据库来源而不是被误展示为知识文档。
     *
     * @param content 原始内容
     * @return 带有图数据库来源标记的内容
     */
    public static Content markAsGraphResult(Content content) {
        Content marked = withMetadata(content, SKIP_RERANK, "true");
        return withMetadata(marked, GRAPH_RESULT, "true");
    }

    /**
     * 判断内容是否标记为跳过重排序/融合。
     */
    public static boolean isSkipRerank(Content content) {
        return hasMetadataValue(content, SKIP_RERANK, "true");
    }

    /**
     * 判断内容是否来自 Neo4j 图数据库。
     */
    public static boolean isGraphResult(Content content) {
        return hasMetadataValue(content, GRAPH_RESULT, "true");
    }

    private static Content withMetadata(Content content, String key, String value) {
        TextSegment originalSegment = content.textSegment();
        Metadata metadata = originalSegment.metadata() != null
                ? Metadata.from(originalSegment.metadata().toMap())
                : new Metadata();
        metadata.put(key, value);
        return Content.from(TextSegment.from(originalSegment.text(), metadata), content.metadata());
    }

    private static boolean hasMetadataValue(Content content, String key, String expectedValue) {
        if (content == null || content.textSegment() == null || content.textSegment().metadata() == null) {
            return false;
        }
        return expectedValue.equals(String.valueOf(content.textSegment().metadata().toMap().get(key)));
    }
}
