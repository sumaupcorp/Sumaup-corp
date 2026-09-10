import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/config/env.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../application/taxi_providers.dart';
import '../domain/receipt_request.dart';
import 'receipt_detail_screen.dart';

/// Modulo Comprobantes del taxista (Premium): su QR + solicitudes de clientes.
class ComprobantesScreen extends ConsumerWidget {
  const ComprobantesScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final token = ref.watch(qrTokenProvider);
    final requests = ref.watch(taxiRequestsProvider);

    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(qrTokenProvider);
        ref.invalidate(taxiRequestsProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!embedded) ...[const Text('Comprobantes', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],
          token.when(
            loading: () => const _QrSkeleton(),
            error: (_, __) => const _QrSkeleton(),
            data: (t) => _QrCard(token: t),
          ),
          const SizedBox(height: AppSpacing.xl),
          const Text('Solicitudes de clientes', style: AppText.title),
          const SizedBox(height: AppSpacing.md),
          requests.when(
            loading: () => const Center(child: Padding(padding: EdgeInsets.all(AppSpacing.xl), child: CircularProgressIndicator(color: AppColors.primary))),
            error: (e, __) => const Text('No pudimos cargar tus solicitudes.', style: AppText.small),
            data: (list) => list.isEmpty
                ? _empty()
                : Column(children: list.map((r) => _RequestCard(request: r)).toList()),
          ),
        ],
      ),
    );

    if (embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Comprobantes')),
      body: SafeArea(child: body),
    );
  }

  Widget _empty() => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpacing.xxl),
        decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
        child: const Text('Aun no tienes solicitudes. Comparte tu QR con tus clientes.',
            textAlign: TextAlign.center, style: AppText.small),
      );
}

class _QrCard extends ConsumerWidget {
  const _QrCard({required this.token});
  final String token;

  Future<void> _regenerate(BuildContext context, WidgetRef ref) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Regenerar tu QR'),
        content: const Text(
            'Tu QR actual dejara de funcionar y tendras que compartir el nuevo. Continuar?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Regenerar')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(taxiRepositoryProvider).regenerateQr();
      ref.invalidate(qrTokenProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('QR regenerado.')));
      }
    } on AppException catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final url = Env.qrUrl(token);
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        children: [
          const Text('Tu QR para clientes', style: AppText.title),
          const SizedBox(height: 4),
          const Text('Tu cliente lo escanea y solicita su boleta o factura.',
              textAlign: TextAlign.center, style: AppText.small),
          const SizedBox(height: AppSpacing.lg),
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(AppSpacing.radius)),
            child: token.isEmpty
                ? const SizedBox(width: 180, height: 180, child: Center(child: CircularProgressIndicator(color: AppColors.primary)))
                : QrImageView(data: url, version: QrVersions.auto, size: 180),
          ),
          const SizedBox(height: AppSpacing.md),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              OutlinedButton.icon(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: url));
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enlace copiado.')));
                },
                icon: const AppIcon('folder-share', size: 18, color: AppColors.primary),
                label: const Text('Copiar enlace'),
              ),
              const SizedBox(width: AppSpacing.sm),
              TextButton.icon(
                onPressed: token.isEmpty ? null : () => _regenerate(context, ref),
                icon: const AppIcon('history-toggle', size: 18, color: AppColors.muted),
                label: const Text('Regenerar', style: TextStyle(color: AppColors.muted)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QrSkeleton extends StatelessWidget {
  const _QrSkeleton();
  @override
  Widget build(BuildContext context) => Container(
        height: 260,
        decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
        alignment: Alignment.center,
        child: const CircularProgressIndicator(color: AppColors.primary),
      );
}

class _RequestCard extends ConsumerWidget {
  const _RequestCard({required this.request});
  final ReceiptRequestModel request;

  void _openDetail(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => ReceiptDetailScreen(request: request)),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final r = request;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          onTap: () => _openDetail(context),
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text('${r.tipo ?? 'Comprobante'} · ${Fmt.money(r.montoFinal)}', style: AppText.bodyStrong),
                    ),
                    _EstadoBadge(estado: r.estado),
                    const SizedBox(width: 6),
                    const Icon(Icons.chevron_right, color: AppColors.muted, size: 20),
                  ],
                ),
                const SizedBox(height: 4),
                Text([r.customerName, r.docNumber].where((e) => (e ?? '').isNotEmpty).join(' · '),
                    style: AppText.small),
                if (r.isPendiente) ...[
                  const SizedBox(height: AppSpacing.md),
                  Row(
                    children: [
                      Expanded(child: _btn(context, ref, 'Confirmar', AppColors.success, () => _confirm(context, ref))),
                      const SizedBox(width: 8),
                      Expanded(child: _btn(context, ref, 'Editar', AppColors.primary, () => _edit(context, ref))),
                      const SizedBox(width: 8),
                      Expanded(child: _btn(context, ref, 'Rechazar', AppColors.danger, () => _reject(context, ref))),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _btn(BuildContext context, WidgetRef ref, String label, Color color, VoidCallback onTap) {
    return SizedBox(
      height: 38,
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(
          foregroundColor: color,
          side: BorderSide(color: color.withValues(alpha: 0.5)),
          padding: EdgeInsets.zero,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
        ),
        child: Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
      ),
    );
  }

  Future<void> _confirm(BuildContext context, WidgetRef ref) async {
    await _run(context, ref, () => ref.read(taxiRepositoryProvider).confirm(request.id));
  }

  Future<void> _reject(BuildContext context, WidgetRef ref) async {
    final motivo = await _askText(context, 'Motivo del rechazo');
    if (motivo == null || !context.mounted) return;
    await _run(context, ref, () => ref.read(taxiRepositoryProvider).reject(request.id, motivo));
  }

  Future<void> _edit(BuildContext context, WidgetRef ref) async {
    final res = await _askMonto(context);
    if (res == null || !context.mounted) return;
    await _run(context, ref, () => ref.read(taxiRepositoryProvider).editMonto(request.id, res.$1, res.$2));
  }

  Future<void> _run(BuildContext context, WidgetRef ref, Future<void> Function() action) async {
    try {
      await action();
      ref.invalidate(taxiRequestsProvider);
    } on AppException catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  Future<String?> _askText(BuildContext context, String title) async {
    final c = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: TextField(controller: c, decoration: const InputDecoration(hintText: 'Escribe el motivo')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, c.text.trim()), child: const Text('Aceptar')),
        ],
      ),
    );
  }

  Future<(double, String?)?> _askMonto(BuildContext context) async {
    final monto = TextEditingController();
    final motivo = TextEditingController();
    return showDialog<(double, String?)>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Editar monto'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: monto, keyboardType: TextInputType.number, decoration: const InputDecoration(hintText: 'Nuevo monto')),
            TextField(controller: motivo, decoration: const InputDecoration(hintText: 'Motivo (opcional)')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(
            onPressed: () {
              final v = double.tryParse(monto.text.replaceAll(',', '.'));
              if (v != null && v > 0) Navigator.pop(context, (v, motivo.text.trim().isEmpty ? null : motivo.text.trim()));
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }
}

class _EstadoBadge extends StatelessWidget {
  const _EstadoBadge({required this.estado});
  final String? estado;

  @override
  Widget build(BuildContext context) {
    final (label, color) = _map(estado);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(AppSpacing.radiusPill)),
      child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700)),
    );
  }

  (String, Color) _map(String? e) {
    switch (e) {
      case 'PENDIENTE_TAXISTA':
        return ('Pendiente', AppColors.warning);
      case 'CONFIRMADO_TAXISTA':
      case 'MONTO_EDITADO_CONFIRMADO':
        return ('Confirmado', AppColors.primary);
      case 'RECHAZADO_TAXISTA':
      case 'CANCELADO':
        return ('Rechazado', AppColors.danger);
      case 'PENDIENTE_BACKOFFICE':
      case 'EN_PROCESO_BACKOFFICE':
        return ('En proceso', AppColors.accent);
      case 'COMPLETADO':
        return ('Completado', AppColors.success);
      case 'OBSERVADO':
        return ('Observado', AppColors.warning);
      default:
        return (e ?? '—', AppColors.muted);
    }
  }
}
