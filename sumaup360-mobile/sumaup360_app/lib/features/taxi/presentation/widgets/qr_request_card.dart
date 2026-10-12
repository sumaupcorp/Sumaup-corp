import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/errors/app_exception.dart';
import '../../../../core/utils/formatters.dart';
import '../../application/taxi_providers.dart';
import '../../domain/receipt_request.dart';
import '../receipt_detail_screen.dart';

/// Tarjeta de solicitud generada por QR (directa de cliente/pasajero).
class QrRequestCard extends ConsumerWidget {
  const QrRequestCard({super.key, required this.request});

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
                      child: Text(
                        '${r.tipo ?? 'Comprobante'} · ${Fmt.money(r.montoFinal)}',
                        style: AppText.bodyStrong,
                      ),
                    ),
                    QrEstadoBadge(estado: r.estado),
                    const SizedBox(width: 6),
                    const Icon(Icons.chevron_right, color: AppColors.muted, size: 20),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  [r.customerName, r.docNumber].where((e) => (e ?? '').isNotEmpty).join(' · '),
                  style: AppText.small,
                ),
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
      ref.invalidate(qrRequestsProvider);
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

class QrEstadoBadge extends StatelessWidget {
  const QrEstadoBadge({super.key, required this.estado});
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
