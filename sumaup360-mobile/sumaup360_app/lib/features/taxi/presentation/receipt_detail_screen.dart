import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/utils/formatters.dart';
import '../application/taxi_providers.dart';
import '../domain/attachment.dart';
import '../domain/receipt_request.dart';
import 'pdf_preview_screen.dart';

/// Detalle de una solicitud del taxista: datos completos + comprobantes adjuntos
/// (con previsualizar / descargar) que subio el equipo de SUMAUP360.
class ReceiptDetailScreen extends ConsumerWidget {
  const ReceiptDetailScreen({super.key, required this.request});
  final ReceiptRequestModel request;

  String get _tipoLabel => request.tipo == 'FACTURA' ? 'Factura' : 'Boleta';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final r = request;
    final attachments = ref.watch(taxiAttachmentsProvider(r.id));

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: Text('$_tipoLabel de ${r.customerName ?? 'cliente'}')),
      body: SafeArea(
        child: RefreshIndicator(
          color: AppColors.primary,
          onRefresh: () async => ref.invalidate(taxiAttachmentsProvider(r.id)),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, AppSpacing.xl),
            children: [
              _HeaderCard(request: r, tipoLabel: _tipoLabel),
              const SizedBox(height: AppSpacing.xl),
              const Text('Datos de la solicitud', style: AppText.title),
              const SizedBox(height: AppSpacing.md),
              _DetailCard(request: r),
              const SizedBox(height: AppSpacing.xl),
              const Text('Comprobantes', style: AppText.title),
              const SizedBox(height: AppSpacing.md),
              attachments.when(
                loading: () => const Center(
                  child: Padding(padding: EdgeInsets.all(AppSpacing.xl), child: CircularProgressIndicator(color: AppColors.primary)),
                ),
                error: (_, __) => const _EmptyBox(text: 'No pudimos cargar los comprobantes. Desliza para reintentar.'),
                data: (list) => list.isEmpty
                    ? const _EmptyBox(text: 'Aun no hay comprobante disponible. El equipo lo esta procesando; te avisaremos cuando este listo.')
                    : Column(children: list.map((a) => _AttachmentCard(attachment: a)).toList()),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HeaderCard extends StatelessWidget {
  const _HeaderCard({required this.request, required this.tipoLabel});
  final ReceiptRequestModel request;
  final String tipoLabel;

  @override
  Widget build(BuildContext context) {
    final r = request;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(tipoLabel, style: AppText.title)),
              EstadoChip(estado: r.estado),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(Fmt.money(r.montoFinal), style: AppText.h1),
          if (r.montoEditado != null && r.monto != null && r.montoEditado != r.monto) ...[
            const SizedBox(height: 2),
            Text('Monto original: ${Fmt.money(r.monto)}',
                style: AppText.small.copyWith(decoration: TextDecoration.lineThrough)),
          ],
        ],
      ),
    );
  }
}

class _DetailCard extends StatelessWidget {
  const _DetailCard({required this.request});
  final ReceiptRequestModel request;

  @override
  Widget build(BuildContext context) {
    final r = request;
    final doc = [r.docType, r.docNumber].where((e) => (e ?? '').isNotEmpty).join(' ');
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        children: [
          _row('Cliente', r.customerName),
          _row('Documento', doc.isEmpty ? null : doc),
          _row('WhatsApp', r.whatsapp),
          _row('Correo', r.email),
          _row('Observacion', r.observacion),
          _row('Motivo', r.motivo),
          _row('Solicitado', Fmt.date(DateTime.tryParse(r.createdAt ?? ''))),
          if ((r.completedAt ?? '').isNotEmpty) _row('Completado', Fmt.date(DateTime.tryParse(r.completedAt!))),
          _row('Codigo', r.id, last: true),
        ],
      ),
    );
  }

  Widget _row(String label, String? value, {bool last = false}) {
    if (value == null || value.isEmpty) return const SizedBox.shrink();
    return Container(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      decoration: last
          ? null
          : const BoxDecoration(border: Border(bottom: BorderSide(color: AppColors.border))),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(width: 110, child: Text(label, style: AppText.small)),
          Expanded(child: Text(value, style: AppText.body, textAlign: TextAlign.right)),
        ],
      ),
    );
  }
}

class _AttachmentCard extends StatefulWidget {
  const _AttachmentCard({required this.attachment});
  final AttachmentModel attachment;

  @override
  State<_AttachmentCard> createState() => _AttachmentCardState();
}

class _AttachmentCardState extends State<_AttachmentCard> {
  bool _busy = false;

  /// Previsualiza el PDF dentro de la app (visor embebido).
  void _preview() {
    final a = widget.attachment;
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => PdfPreviewScreen(url: a.fileUrl, title: a.fileName)),
    );
  }

  /// Abre el PDF con una app externa (navegador / visor del sistema) para descargarlo.
  Future<void> _download() async {
    setState(() => _busy = true);
    try {
      final uri = Uri.parse(widget.attachment.fileUrl);
      final ok = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!ok && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo abrir el archivo.')));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo abrir el archivo.')));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final a = widget.attachment;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(AppSpacing.radius),
                ),
                child: const Icon(Icons.picture_as_pdf_outlined, color: AppColors.primary),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(a.fileName ?? 'Comprobante.pdf', style: AppText.bodyStrong, maxLines: 2, overflow: TextOverflow.ellipsis),
                    if ((a.createdAt ?? '').isNotEmpty)
                      Text(Fmt.date(DateTime.tryParse(a.createdAt!)), style: AppText.small),
                  ],
                ),
              ),
              IconButton(
                tooltip: 'Copiar enlace',
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: a.fileUrl));
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enlace copiado.')));
                },
                icon: const Icon(Icons.link, color: AppColors.muted, size: 20),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _busy ? null : _preview,
                  icon: const Icon(Icons.visibility_outlined, size: 18),
                  label: const Text('Previsualizar'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primary,
                    side: BorderSide(color: AppColors.primary.withValues(alpha: 0.5)),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: FilledButton.icon(
                  onPressed: _busy ? null : _download,
                  icon: _busy
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.download_outlined, size: 18),
                  label: const Text('Descargar'),
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _EmptyBox extends StatelessWidget {
  const _EmptyBox({required this.text});
  final String text;

  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpacing.xxl),
        decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
        child: Text(text, textAlign: TextAlign.center, style: AppText.small),
      );
}

/// Chip de estado (mismo criterio de color que la lista de solicitudes).
class EstadoChip extends StatelessWidget {
  const EstadoChip({super.key, required this.estado});
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
