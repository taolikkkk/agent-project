package cn.hollis.llm.mentor.know.engine.chat.entity;

import cn.hollis.llm.mentor.know.engine.ai.model.IntentRecognitionResult;
import cn.hollis.llm.mentor.know.engine.chat.constant.ChatSource;

public record ChatParam(String userId, String conversationId, String messageId, String content, String assistantMessageId,
                        IntentRecognitionResult intentRecognitionResult, ChatSource chatSource) {
}
