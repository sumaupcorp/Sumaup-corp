package com.sumaup360.chatbot.dto;

import com.sumaup360.chatbot.domain.Conversation;
import com.sumaup360.chatbot.domain.Message;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ChatbotDtos {

    private ChatbotDtos() {
    }

    public record SendMessageRequest(UUID conversationId, @NotBlank String text) {
    }

    public record ChatReplyResponse(UUID conversationId, String reply, String mode, boolean blocked,
                                    int remaining, int limit) {
    }

    /** Estado de uso de IA del usuario. */
    public record AiUsageResponse(int used, int limit, int remaining, boolean blocked, String periodo, boolean premium) {
    }

    public record ConversationView(UUID id, String title, OffsetDateTime createdAt) {
        public static ConversationView from(Conversation c) {
            return new ConversationView(c.getId(), c.getTitle(), c.getCreatedAt());
        }
    }

    public record MessageView(String role, String content, OffsetDateTime createdAt) {
        public static MessageView from(Message m) {
            return new MessageView(m.getRole(), m.getContent(), m.getCreatedAt());
        }
    }
}
