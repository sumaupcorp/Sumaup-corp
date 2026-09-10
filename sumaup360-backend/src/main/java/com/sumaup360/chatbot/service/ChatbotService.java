package com.sumaup360.chatbot.service;

import com.sumaup360.app.ai.AiPrompt;
import com.sumaup360.app.ai.AiPromptService;
import com.sumaup360.app.ai.AiUsageService;
import com.sumaup360.app.ai.LlmClient;
import com.sumaup360.app.ai.UserAiContextService;
import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.service.ProfileCompletionService;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.chatbot.domain.Conversation;
import com.sumaup360.chatbot.domain.Message;
import com.sumaup360.chatbot.repository.ConversationRepository;
import com.sumaup360.chatbot.repository.MessageRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chatbot Suma. Reglas de IA:
 *  - perfil incompleto -> respuestas guiadas (no consumen credito), enfocadas en completar perfil.
 *  - perfil completo -> consulta a Gemini con limite (free/premium configurable); si no hay
 *    Gemini configurado, cae al generador de reglas. Cada consulta queda registrada.
 */
@Service
public class ChatbotService {

    public record ChatResult(UUID conversationId, String reply, String mode, boolean blocked,
                             int remaining, int limit) {
    }

    private static final String BLOCK_MSG =
            "Has usado tus consultas gratuitas. Activa tu plan Premium para seguir usando el asesor IA de SUMAUP360.";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SumaReplyGenerator replyGenerator;
    private final ProfileCompletionService completionService;
    private final AiUsageService aiUsageService;
    private final LlmClient llm;
    private final AiPromptService aiPromptService;
    private final UserAiContextService userContextService;
    private final SubscriptionService subscriptionService;
    private final PersonProfileRepository profileRepository;

    public ChatbotService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          SumaReplyGenerator replyGenerator,
                          ProfileCompletionService completionService,
                          AiUsageService aiUsageService,
                          LlmClient llm,
                          AiPromptService aiPromptService,
                          UserAiContextService userContextService,
                          SubscriptionService subscriptionService,
                          PersonProfileRepository profileRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.replyGenerator = replyGenerator;
        this.completionService = completionService;
        this.aiUsageService = aiUsageService;
        this.llm = llm;
        this.aiPromptService = aiPromptService;
        this.userContextService = userContextService;
        this.subscriptionService = subscriptionService;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ChatResult sendMessage(UUID userId, UUID conversationId, String text) {
        Conversation conversation = (conversationId == null)
                ? newConversation(userId, text)
                : conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversacion no encontrada."));
        save(conversation.getId(), "USER", text);

        String planActivo = subscriptionService.activePlanCode(userId);

        // 1) Perfil incompleto -> modo guiado (no consume credito).
        if (!completionService.status(userId).completed()) {
            String reply = guidedReply();
            save(conversation.getId(), "ASSISTANT", reply);
            aiUsageService.record(userId, text, reply, "guiado", null, "GUIADO", false, planActivo);
            return new ChatResult(conversation.getId(), reply, "GUIADO", false, 0, 0);
        }

        // 2) Limite de IA.
        AiUsageService.UsageStatus usage = aiUsageService.status(userId);
        if (usage.blocked()) {
            save(conversation.getId(), "ASSISTANT", BLOCK_MSG);
            aiUsageService.record(userId, text, BLOCK_MSG, null, null, "CHAT_IA", false, planActivo);
            return new ChatResult(conversation.getId(), BLOCK_MSG, "BLOQUEADO", true, 0, usage.limit());
        }

        // 3) IA (OpenAI/Gemini segun config; fallback de reglas si no esta configurada).
        AiPrompt prompt = aiPromptService.get();
        Optional<String> ai = llm.generate(
                systemInstruction(userId, prompt), text, prompt.getTemperature(), prompt.getMaxTokens());
        boolean usedAi = ai.isPresent();
        String reply = ai.orElseGet(() -> replyGenerator.reply(text));
        String modelo = usedAi ? llm.model() : "suma-rules";
        save(conversation.getId(), "ASSISTANT", reply);
        // Solo consume credito si respondio la IA real (no el fallback de reglas).
        aiUsageService.record(userId, text, reply, modelo, null, "CHAT_IA", usedAi, planActivo);

        AiUsageService.UsageStatus after = aiUsageService.status(userId);
        return new ChatResult(conversation.getId(), reply, "IA", false, after.remaining(), after.limit());
    }

    @Transactional(readOnly = true)
    public AiUsageService.UsageStatus usage(UUID userId) {
        return aiUsageService.status(userId);
    }

    @Transactional(readOnly = true)
    public List<Conversation> conversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Message> messages(UUID userId, UUID conversationId) {
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion no encontrada."));
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    private String guidedReply() {
        return "Hola, soy Suma. Para ayudarte con tus impuestos primero completa tu perfil: "
                + "ingresa tu DNI o RUC para validar tu informacion en SUNAT y elige si eres taxista "
                + "o repartidor. Cuando tu RUC este ACTIVO y HABIDO podre asesorarte al detalle.";
    }

    /**
     * System prompt del chat = base configurable (backoffice) + contexto del tipo de trabajador
     * + DATOS COMPLETOS del usuario (perfil, plan, estadisticas, conteos). Asi la IA responde
     * con precision sobre su nombre, DNI, RUC, plan, comprobantes, estadisticas, etc.
     */
    private String systemInstruction(UUID userId, AiPrompt prompt) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        ClientType ct = p != null ? p.getClientType() : null;
        StringBuilder sb = new StringBuilder(prompt.getChatSystem());
        String caseContext = prompt.contextFor(ct);
        if (!caseContext.isBlank()) {
            sb.append("\n\n").append(caseContext);
        }
        sb.append("\n\n").append(userContextService.build(userId));
        return sb.toString();
    }

    private Conversation newConversation(UUID userId, String firstText) {
        Conversation c = new Conversation();
        c.setUserId(userId);
        String title = firstText == null ? "Conversacion"
                : firstText.length() > 40 ? firstText.substring(0, 40) : firstText;
        c.setTitle(title);
        return conversationRepository.save(c);
    }

    private void save(UUID conversationId, String role, String content) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        messageRepository.save(m);
    }
}
