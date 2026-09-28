package cn.hollis.llm.mentor.know.engine.rag.modules;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowEngineQueryRouterTest {
    @Test
    void validQwenDecisionSelectsKnowledgeRetrieverInsteadOfFallingBackToAll() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("""
                {"intent":"汽车保养预约","strategy":"knowledge_base","confidence":0.85,"reasoning":"“星河验证站”的预约暗号属于非结构化服务信息"}
                """)).build());
        ContentRetriever knowledge = new ProgressAwareContentRetriever(mock(KnowEngineElasticsearchContentRetriever.class), null);
        ContentRetriever sql = mock(KnowEngineSqlDatabaseContentRetriever.class);
        ContentRetriever graph = mock(KnowEngineNeo4jContentRetriever.class);
        KnowEngineQueryRouter router = new KnowEngineQueryRouter(List.of(knowledge, sql, graph), model);
        assertEquals(List.of(knowledge), router.route(Query.from("汽车保养如何预约？")));
    }

    @Test
    void modelFailureKeepsRetrievalAvailable() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("model unavailable"));
        List<ContentRetriever> retrievers = List.of(mock(ContentRetriever.class));
        assertEquals(retrievers, new KnowEngineQueryRouter(retrievers, model).route(Query.from("保养周期")));
    }
}
