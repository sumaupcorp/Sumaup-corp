import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../profile/application/profile_providers.dart';
import '../application/chatbot_providers.dart';
import '../domain/chat_models.dart';

/// Asesor Suma IA: chat conectado al backend (Gemini con limites; si no hay cuota,
/// responde con reglas). Se usa como pestana (embedded) y como pantalla.
class ChatScreen extends ConsumerStatefulWidget {
  const ChatScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final _focus = FocusNode();
  final List<ChatMessage> _messages = [];
  String? _conversationId;
  AiUsage? _usage;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _loadUsage();
  }

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    _focus.dispose();
    super.dispose();
  }

  /// Envia una pregunta sugerida (chip) directamente.
  void _sendSuggestion(String text) {
    if (_sending) return;
    _input.text = text;
    _send();
  }

  /// Preguntas frecuentes por tipo de trabajador (para no gastar consultas en dudas vagas).
  List<String> _suggestionsFor(String? clientType) {
    switch (clientType) {
      case 'TAXISTA':
        return const [
          'En que regimen deberia estar como taxista?',
          'Cuanto voy a pagar de impuestos?',
          'Como emito mis comprobantes a mis clientes?',
          'Que gastos puedo usar para pagar menos?',
        ];
      case 'DELIVERY_PEYA':
        return const [
          'Que regimen me conviene como repartidor?',
          'Como declaro mis ingresos de delivery?',
          'Cuanto pago de impuestos al mes?',
          'Necesito RUC para trabajar en delivery?',
        ];
      case 'SERVICIOS_PROFESIONALES':
        return const [
          'Me conviene suspender la 4ta categoria?',
          'Como emito mis recibos por honorarios?',
          'Cuanto me retienen de 4ta categoria?',
          'Que regimen me conviene como profesional?',
        ];
      default:
        return const [
          'En que regimen tributario deberia estar?',
          'Cuanto voy a pagar de impuestos?',
          'Que necesito para estar en regla con SUNAT?',
        ];
    }
  }

  Future<void> _loadUsage() async {
    try {
      final u = await ref.read(chatbotRepositoryProvider).usage();
      if (mounted) setState(() => _usage = u);
    } catch (_) {/* silencioso */}
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty || _sending) return;
    setState(() {
      _messages.add(ChatMessage(role: 'USER', content: text));
      _sending = true;
      _input.clear();
    });
    _scrollToEnd();
    try {
      final reply = await ref.read(chatbotRepositoryProvider).send(conversationId: _conversationId, text: text);
      if (!mounted) return;
      setState(() {
        _conversationId = reply.conversationId ?? _conversationId;
        _messages.add(ChatMessage(role: 'ASSISTANT', content: reply.reply));
        // Actualiza el contador con lo que devuelve el envio.
        if (reply.mode != 'GUIADO') {
          _usage = AiUsage(
            used: (_usage?.limit ?? reply.limit) - reply.remaining,
            limit: reply.limit,
            remaining: reply.remaining,
            blocked: reply.blocked,
            premium: _usage?.premium ?? false,
            periodo: _usage?.periodo,
          );
        }
      });
    } on AppException catch (e) {
      if (mounted) setState(() => _messages.add(ChatMessage(role: 'ASSISTANT', content: e.message)));
    } catch (_) {
      if (mounted) {
        setState(() => _messages.add(const ChatMessage(role: 'ASSISTANT', content: 'No pude responder. Intenta de nuevo.')));
      }
    } finally {
      if (mounted) setState(() => _sending = false);
      _scrollToEnd();
    }
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(_scroll.position.maxScrollExtent,
            duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final blocked = _usage?.blocked ?? false;
    final clientType = ref.watch(profileMeProvider).valueOrNull?.clientType;
    final body = Column(
      children: [
        _UsageBar(usage: _usage),
        Expanded(
          child: _messages.isEmpty
              ? _WelcomeWithSuggestions(
                  suggestions: _suggestionsFor(clientType),
                  onPick: _sendSuggestion,
                  onOther: () => _focus.requestFocus(),
                )
              : ListView.builder(
                  controller: _scroll,
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  itemCount: _messages.length,
                  itemBuilder: (_, i) => _Bubble(message: _messages[i]),
                ),
        ),
        if (_sending)
          const Padding(
            padding: EdgeInsets.only(bottom: AppSpacing.sm),
            child: Text('Suma esta escribiendo...', style: AppText.small),
          ),
        if (blocked) _BlockedBanner(onUpgrade: () => context.push(Routes.plans)) else _InputBar(
          controller: _input,
          focusNode: _focus,
          sending: _sending,
          onSend: _send,
        ),
      ],
    );

    if (widget.embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Asesor IA')),
      body: SafeArea(child: body),
    );
  }
}

class _UsageBar extends StatelessWidget {
  const _UsageBar({required this.usage});
  final AiUsage? usage;

  @override
  Widget build(BuildContext context) {
    if (usage == null) return const SizedBox.shrink();
    final u = usage!;
    final label = u.premium ? 'Premium' : 'Consultas IA: ${u.remaining}/${u.limit}';
    final color = u.blocked ? AppColors.danger : AppColors.primary;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      color: color.withValues(alpha: 0.08),
      child: Row(
        children: [
          AppIcon(u.premium ? 'crown' : 'help-small', size: 16, color: color),
          const SizedBox(width: 6),
          Text(label, style: AppText.small.copyWith(color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

class _WelcomeWithSuggestions extends StatelessWidget {
  const _WelcomeWithSuggestions({required this.suggestions, required this.onPick, required this.onOther});
  final List<String> suggestions;
  final ValueChanged<String> onPick;
  final VoidCallback onOther;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(AppSpacing.xl),
      children: [
        const SizedBox(height: AppSpacing.md),
        Center(
          child: Column(
            children: [
              Container(
                width: 64, height: 64,
                decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(20)),
                alignment: Alignment.center,
                child: const AppIcon('help-small', size: 34, color: AppColors.primary),
              ),
              const SizedBox(height: AppSpacing.md),
              const Text('Hola, soy Suma', style: AppText.h2),
              const SizedBox(height: 6),
              const Text(
                'Elige una pregunta frecuente o escribe la tuya. Asi aprovechas mejor tus consultas.',
                textAlign: TextAlign.center, style: AppText.body,
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.xl),
        ...suggestions.map((q) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: _SuggestionChip(text: q, onTap: () => onPick(q)),
            )),
        Padding(
          padding: const EdgeInsets.only(bottom: AppSpacing.sm),
          child: _SuggestionChip(text: 'Otros (escribir mi pregunta)', onTap: onOther, outlined: true),
        ),
      ],
    );
  }
}

class _SuggestionChip extends StatelessWidget {
  const _SuggestionChip({required this.text, required this.onTap, this.outlined = false});
  final String text;
  final VoidCallback onTap;
  final bool outlined;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radius),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm + 4),
        decoration: BoxDecoration(
          color: outlined ? AppColors.surfaceAlt : AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          border: Border.all(color: outlined ? AppColors.border : AppColors.primarySoft),
        ),
        child: Row(
          children: [
            AppIcon(outlined ? 'edit' : 'help-small', size: 18, color: AppColors.primary),
            const SizedBox(width: AppSpacing.sm),
            Expanded(child: Text(text, style: AppText.body)),
          ],
        ),
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message});
  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm + 2),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
        decoration: BoxDecoration(
          color: isUser ? AppColors.primary : AppColors.surface,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(isUser ? 16 : 4),
            bottomRight: Radius.circular(isUser ? 4 : 16),
          ),
          border: isUser ? null : Border.all(color: AppColors.border),
        ),
        child: Text(
          message.content,
          style: AppText.body.copyWith(color: isUser ? Colors.white : AppColors.ink),
        ),
      ),
    );
  }
}

class _InputBar extends StatelessWidget {
  const _InputBar({required this.controller, required this.focusNode, required this.sending, required this.onSend});
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool sending;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(AppSpacing.md, AppSpacing.sm, AppSpacing.md, AppSpacing.md),
      decoration: const BoxDecoration(color: AppColors.surface, boxShadow: AppShadows.card),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                focusNode: focusNode,
                minLines: 1,
                maxLines: 4,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => onSend(),
                decoration: InputDecoration(
                  hintText: 'Escribe tu pregunta...',
                  filled: true,
                  fillColor: AppColors.surfaceAlt,
                  contentPadding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 10),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            GestureDetector(
              onTap: sending ? null : onSend,
              child: Container(
                width: 46, height: 46,
                decoration: const BoxDecoration(color: AppColors.primary, shape: BoxShape.circle),
                alignment: Alignment.center,
                child: const Icon(Icons.send_rounded, color: Colors.white, size: 22),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BlockedBanner extends StatelessWidget {
  const _BlockedBanner({required this.onUpgrade});
  final VoidCallback onUpgrade;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: const BoxDecoration(color: AppColors.surface, boxShadow: AppShadows.card),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Usaste tus consultas gratuitas. Activa tu plan Premium para seguir con el asesor IA.',
              textAlign: TextAlign.center, style: AppText.small,
            ),
            const SizedBox(height: AppSpacing.md),
            SizedBox(
              width: double.infinity, height: 50,
              child: ElevatedButton(
                onPressed: onUpgrade,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                ),
                child: const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    AppIcon('crown', size: 20, color: Colors.white),
                    SizedBox(width: 8),
                    Text('Activar Premium', style: AppText.button),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
