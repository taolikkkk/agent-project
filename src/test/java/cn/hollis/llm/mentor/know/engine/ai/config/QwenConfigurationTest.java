package cn.hollis.llm.mentor.know.engine.ai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class QwenConfigurationTest {
    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> request = new AtomicReference<>();
    private QwenProperties properties;
    private final QwenConfiguration configuration = new QwenConfiguration();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            JsonNode body = mapper.readTree(exchange.getRequestBody());
            request.set(body);
            String response;
            if (body.path("stream").asBoolean()) {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                response = "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"已识别\"}}]}\n\n"
                        + "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
                        + "data: [DONE]\n\n";
            } else {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                response = "{\"id\":\"test\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"已识别\"},\"finish_reason\":\"stop\"}]}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        properties = new QwenProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.setMaxTokens(128);
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void textAndImagesShareQwenConfiguration() {
        var model = configuration.openAiChatModel(properties);
        assertEquals("已识别", model.chat("测试"));
        assertRequest();
        model.chat(UserMessage.from(TextContent.from("识别图片"), ImageContent.from("aGVsbG8=", "image/png")));
        assertRequest();
        assertEquals("data:image/png;base64,aGVsbG8=", request.get().path("messages").get(0).path("content").get(1).path("image_url").path("url").asText());
    }

    @Test
    void structuredRequestsEnableJsonWithoutChangingOtherCalls() {
        var model = configuration.openAiChatModel(properties);
        model.chat(ChatRequest.builder().messages(UserMessage.from("返回 JSON 决策"))
                .responseFormat(ResponseFormat.JSON).build());
        assertEquals("json_object", request.get().path("response_format").path("type").asText());
        model.chat("生成普通标题");
        assertRequest();
    }

    @Test
    void streamingUsesTheSameModelAndLlamaCppThinkingSwitch() throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        configuration.openAiStreamingChatModel(properties).chat("测试", new StreamingChatResponseHandler() {
            @Override public void onPartialResponse(String text) { }
            @Override public void onCompleteResponse(ChatResponse response) { result.complete(response.aiMessage().text()); }
            @Override public void onError(Throwable error) { result.completeExceptionally(error); }
        });
        assertEquals("已识别", result.get(10, TimeUnit.SECONDS));
        assertTrue(request.get().path("stream").asBoolean());
        assertRequest();
    }

    private void assertRequest() {
        assertEquals("qwen3.8-27b", request.get().path("model").asText());
        assertEquals(128, request.get().path("max_tokens").asInt());
        assertFalse(request.get().path("chat_template_kwargs").path("enable_thinking").asBoolean(true));
        assertEquals(0, request.get().path("temperature").asDouble());
        assertFalse(request.get().has("response_format"), "普通对话和图片描述不能被强制转换成 JSON 输出");
    }
}
