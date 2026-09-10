// Modelos del chatbot Suma (IA).

class ChatMessage {
  const ChatMessage({required this.role, required this.content});
  final String role; // USER | ASSISTANT
  final String content;

  bool get isUser => role == 'USER';

  factory ChatMessage.fromJson(Map<String, dynamic> j) =>
      ChatMessage(role: (j['role'] as String?) ?? 'ASSISTANT', content: (j['content'] as String?) ?? '');
}

/// Respuesta a un mensaje enviado.
class ChatReply {
  const ChatReply({
    this.conversationId,
    required this.reply,
    this.mode,
    this.blocked = false,
    this.remaining = 0,
    this.limit = 0,
  });

  final String? conversationId;
  final String reply;
  final String? mode; // GUIADO | IA | BLOQUEADO
  final bool blocked;
  final int remaining;
  final int limit;

  factory ChatReply.fromJson(Map<String, dynamic> j) => ChatReply(
        conversationId: j['conversationId'] as String?,
        reply: (j['reply'] as String?) ?? '',
        mode: j['mode'] as String?,
        blocked: (j['blocked'] as bool?) ?? false,
        remaining: (j['remaining'] as int?) ?? 0,
        limit: (j['limit'] as int?) ?? 0,
      );
}

/// Estado de uso de IA del usuario.
class AiUsage {
  const AiUsage({
    this.used = 0,
    this.limit = 0,
    this.remaining = 0,
    this.blocked = false,
    this.periodo,
    this.premium = false,
  });

  final int used;
  final int limit;
  final int remaining;
  final bool blocked;
  final String? periodo;
  final bool premium;

  factory AiUsage.fromJson(Map<String, dynamic> j) => AiUsage(
        used: (j['used'] as int?) ?? 0,
        limit: (j['limit'] as int?) ?? 0,
        remaining: (j['remaining'] as int?) ?? 0,
        blocked: (j['blocked'] as bool?) ?? false,
        periodo: j['periodo'] as String?,
        premium: (j['premium'] as bool?) ?? false,
      );
}
