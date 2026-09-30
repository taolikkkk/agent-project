package cn.hollis.llm.mentor.know.engine.chat.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ChatMemoryOrderTest {

    @Test
    void shouldKeepSystemMessageBeforeHistoricalMessages() {
        InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        store.updateMessages("conversation", List.of(
                UserMessage.from("历史问题"),
                AiMessage.from("历史回答")
        ));

        ChatMemory memory = MessageWindowChatMemory.builder()
                .id("conversation")
                .maxMessages(10)
                .chatMemoryStore(store)
                .alwaysKeepSystemMessageFirst(true)
                .build();
        memory.add(SystemMessage.from("系统提示词"));

        assertInstanceOf(SystemMessage.class, memory.messages().getFirst());
    }
}
