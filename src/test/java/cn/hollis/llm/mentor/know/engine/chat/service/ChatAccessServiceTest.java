package cn.hollis.llm.mentor.know.engine.chat.service;

import cn.hollis.llm.mentor.know.engine.auth.service.AuthService;
import cn.hollis.llm.mentor.know.engine.chat.entity.ChatConversation;
import cn.hollis.llm.mentor.know.engine.chat.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatAccessServiceTest {
    @Test
    void onlyOwnerCanAccessConversationAndItsMessages() {
        AuthService auth = mock(AuthService.class);
        ChatConversationService conversations = mock(ChatConversationService.class);
        ChatMessageService messages = mock(ChatMessageService.class);
        ChatAccessService access = new ChatAccessService(auth, conversations, messages);
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId("staff_TEST001");
        when(conversations.getByConversationId("conversation")).thenReturn(conversation);
        ChatMessage message = new ChatMessage();
        message.setConversationId("conversation");
        when(messages.getByMessageId("message")).thenReturn(message);
        when(auth.getCurrentUserId()).thenReturn("staff_TEST001");
        assertDoesNotThrow(() -> access.requireMessage("message"));
        when(auth.getCurrentUserId()).thenReturn("customer");
        assertThrows(ResponseStatusException.class, () -> access.requireConversation("conversation"));
        assertThrows(ResponseStatusException.class, () -> access.requireMessage("message"));
        assertThrows(ResponseStatusException.class, () -> access.requireConversation("missing"));
    }
}
