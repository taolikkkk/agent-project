package cn.hollis.llm.mentor.know.engine.rag.modules;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KnowEngineQueryTransformerTest {
    @Test
    void firstQuestionDoesNotRequireHistory() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(anyString())).thenReturn("Tesla Model 3 官方指导价");
        var progress = new ArrayList<String>();
        var queries = new KnowEngineQueryTransformer(model, null, progress::add).transform(Query.from("毛豆3多少钱"));
        assertEquals(1, queries.size());
        assertEquals("Tesla Model 3 官方指导价", queries.iterator().next().text());
        assertEquals(List.of("[PROGRESS]:正在优化您的问题..."), progress);
    }

    @Test
    void rewriteRetainsHistoryAndPermissionsMetadata() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(anyString())).thenReturn("车辆保养周期");
        Metadata metadata = Metadata.builder().chatMessage(UserMessage.from("多久保养")).invocationContext(dev.langchain4j.invocation.InvocationContext.builder().chatMemoryId("test").build()).chatMemory(List.of(UserMessage.from("我的车是 Model 3"))).build();
        var result = new KnowEngineQueryTransformer(model, null).transform(Query.from("多久保养", metadata));
        assertSame(metadata, result.iterator().next().metadata());
        verify(model).chat(contains("我的车是 Model 3"));
    }

    @Test
    void blankRewritePreservesOriginalQuestion() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(anyString())).thenReturn("  ");
        var result = new KnowEngineQueryTransformer(model, null).transform(Query.from("多久保养"));
        assertEquals("多久保养", result.iterator().next().text());
    }

    @Test
    void modelErrorsRemainVisible() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(anyString())).thenThrow(new IllegalStateException("模型不可用"));
        var error = assertThrows(IllegalStateException.class,
                () -> new KnowEngineQueryTransformer(model, null).transform(Query.from("多久保养")));
        assertEquals("模型不可用", error.getMessage());
    }
}
