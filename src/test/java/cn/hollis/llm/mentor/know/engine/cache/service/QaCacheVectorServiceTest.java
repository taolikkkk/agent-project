package cn.hollis.llm.mentor.know.engine.cache.service;

import cn.hollis.llm.mentor.know.engine.cache.config.QaCacheProperties;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class QaCacheVectorServiceTest {
    @Test
    void freshDatabaseWithoutCacheIndexDoesNotCallEmbeddingModel() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/know-engine-qa-cache-v1", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try (RestClient client = RestClient.builder(new HttpHost("127.0.0.1", server.getAddress().getPort())).build()) {
            OpenAiEmbeddingModel model = mock(OpenAiEmbeddingModel.class);
            QaCacheVectorService service = new QaCacheVectorService(model, client, new QaCacheProperties());
            assertTrue(service.search("车辆保养周期", 0.95).isEmpty());
            verifyNoInteractions(model);
        } finally { server.stop(0); }
    }
}
