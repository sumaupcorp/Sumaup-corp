package com.sumaup360.chatbot.web;

import com.sumaup360.app.ai.AiUsageService;
import com.sumaup360.chatbot.dto.ChatbotDtos.AiUsageResponse;
import com.sumaup360.chatbot.dto.ChatbotDtos.ChatReplyResponse;
import com.sumaup360.chatbot.dto.ChatbotDtos.ConversationView;
import com.sumaup360.chatbot.dto.ChatbotDtos.MessageView;
import com.sumaup360.chatbot.dto.ChatbotDtos.SendMessageRequest;
import com.sumaup360.chatbot.service.ChatbotService;
import com.sumaup360.chatbot.service.ChatbotService.ChatResult;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Chatbot Suma del usuario autenticado. */
@RestController
@RequestMapping("/api/v1/chatbot")
@Tag(name = "Chatbot Suma", description = "Asistente conversacional")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/messages")
    @Operation(summary = "Envia un mensaje a Suma y recibe su respuesta")
    public ChatReplyResponse send(@Valid @RequestBody SendMessageRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        ChatResult r = chatbotService.sendMessage(userId, req.conversationId(), req.text());
        return new ChatReplyResponse(r.conversationId(), r.reply(), r.mode(), r.blocked(), r.remaining(), r.limit());
    }

    @GetMapping("/usage")
    @Operation(summary = "Consultas IA usadas/disponibles del usuario")
    public AiUsageResponse usage() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        AiUsageService.UsageStatus u = chatbotService.usage(userId);
        return new AiUsageResponse(u.used(), u.limit(), u.remaining(), u.blocked(), u.periodo(), u.premium());
    }

    @GetMapping("/conversations")
    @Operation(summary = "Lista mis conversaciones")
    public List<ConversationView> conversations() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return chatbotService.conversations(userId).stream().map(ConversationView::from).toList();
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Mensajes de una conversacion")
    public List<MessageView> messages(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return chatbotService.messages(userId, id).stream().map(MessageView::from).toList();
    }
}
