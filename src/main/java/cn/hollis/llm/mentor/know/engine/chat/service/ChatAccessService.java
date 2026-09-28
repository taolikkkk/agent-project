package cn.hollis.llm.mentor.know.engine.chat.service;

import cn.hollis.llm.mentor.know.engine.auth.service.AuthService;
import cn.hollis.llm.mentor.know.engine.chat.entity.ChatConversation;
import cn.hollis.llm.mentor.know.engine.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Web 会话和消息接口统一检查当前登录人，避免员工与客户串读对话。 */
@Service
@RequiredArgsConstructor
public class ChatAccessService {
    private final AuthService authService;
    private final ChatConversationService conversations;
    private final ChatMessageService messages;

    public void requireConversation(String conversationId) {
        ChatConversation conversation = conversations.getByConversationId(conversationId);
        if (conversation == null || !authService.getCurrentUserId().equals(conversation.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }
    }

    public void requireMessage(String messageId) {
        ChatMessage message = messages.getByMessageId(messageId);
        if (message == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "消息不存在");
        requireConversation(message.getConversationId());
    }
}
