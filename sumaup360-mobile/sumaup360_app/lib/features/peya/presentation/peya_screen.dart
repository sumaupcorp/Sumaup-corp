import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_icon.dart';
import '../application/peya_providers.dart';
import '../domain/peya_upload.dart';

/// Modulo Peya (Premium): sube tu PDF mensual de ventas y sigue su procesamiento.
class PeyaScreen extends ConsumerStatefulWidget {
  const PeyaScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  ConsumerState<PeyaScreen> createState() => _PeyaScreenState();
}

class _PeyaScreenState extends ConsumerState<PeyaScreen> {
  bool _uploading = false;

  String _currentPeriod() {
    final n = DateTime.now();
    return '${n.year}-${n.month.toString().padLeft(2, '0')}';
  }

  Future<void> _pickAndUpload() async {
    try {
      final result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['pdf']);
      final path = result?.files.single.path;
      if (path == null) return;
      setState(() => _uploading = true);
      final repo = ref.read(peyaRepositoryProvider);
      final url = await repo.uploadPdf(File(path));
      await repo.create(periodo: _currentPeriod(), pdfUrl: url);
      ref.invalidate(peyaUploadsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('PDF subido. SUMAUP360 preparara tu reporte.')));
      }
    } on AppException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo subir el PDF.')));
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final uploads = ref.watch(peyaUploadsProvider);
    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async => ref.invalidate(peyaUploadsProvider),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!widget.embedded) ...[const Text('Peya', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],
          _UploadCard(period: _currentPeriod(), uploading: _uploading, onPick: _pickAndUpload),
          const SizedBox(height: AppSpacing.xl),
          const Text('Tus cargas', style: AppText.title),
          const SizedBox(height: AppSpacing.md),
          uploads.when(
            loading: () => const Center(child: Padding(padding: EdgeInsets.all(AppSpacing.xl), child: CircularProgressIndicator(color: AppColors.primary))),
            error: (_, __) => const Text('No pudimos cargar tus reportes.', style: AppText.small),
            data: (list) => list.isEmpty
                ? const _Empty()
                : Column(children: list.map((u) => _UploadTile(upload: u)).toList()),
          ),
        ],
      ),
    );

    if (widget.embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Peya')),
      body: SafeArea(child: body),
    );
  }
}

class _UploadCard extends StatelessWidget {
  const _UploadCard({required this.period, required this.uploading, required this.onPick});
  final String period;
  final bool uploading;
  final VoidCallback onPick;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Sube tus ventas de $period', style: AppText.title),
          const SizedBox(height: 4),
          const Text('Carga el PDF de detalle de ventas de Peya y SUMAUP360 prepara tu reporte y declaracion.',
              style: AppText.small),
          const SizedBox(height: AppSpacing.lg),
          SizedBox(
            height: 50,
            child: ElevatedButton(
              onPressed: uploading ? null : onPick,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
              ),
              child: uploading
                  ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                  : const Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                      AppIcon('file-import', size: 20, color: Colors.white),
                      SizedBox(width: 8),
                      Text('Subir PDF de ventas', style: AppText.button),
                    ]),
            ),
          ),
        ],
      ),
    );
  }
}

class _Empty extends StatelessWidget {
  const _Empty();
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpacing.xxl),
        decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
        child: const Text('Aun no subes ninguna carga.', textAlign: TextAlign.center, style: AppText.small),
      );
}

class _UploadTile extends ConsumerWidget {
  const _UploadTile({required this.upload});
  final PeyaUpload upload;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final u = upload;
    final files = ref.watch(peyaFilesProvider(u.id));
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text('Periodo ${u.periodo ?? ''}', style: AppText.bodyStrong)),
              _Badge(estado: u.estado),
            ],
          ),
          if ((u.observacionBackoffice ?? '').isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(u.observacionBackoffice!, style: AppText.small),
          ],
          if ((u.codigoNps ?? '').isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            GestureDetector(
              onTap: () {
                Clipboard.setData(ClipboardData(text: u.codigoNps!));
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Codigo NPS copiado.')));
              },
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 8),
                decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(AppSpacing.radius)),
                child: Row(children: [
                  const AppIcon('receipt-tax', size: 16, color: AppColors.primary),
                  const SizedBox(width: 6),
                  Text('Codigo NPS: ${u.codigoNps}', style: AppText.small.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600)),
                ]),
              ),
            ),
          ],
          files.maybeWhen(
            data: (list) => list.isEmpty
                ? const SizedBox.shrink()
                : Padding(
                    padding: const EdgeInsets.only(top: AppSpacing.sm),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: list.map((f) => InkWell(
                            onTap: () {
                              Clipboard.setData(ClipboardData(text: f.fileUrl));
                              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enlace del archivo copiado.')));
                            },
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(children: [
                                const AppIcon('download', size: 16, color: AppColors.primary),
                                const SizedBox(width: 6),
                                Expanded(child: Text(f.fileName ?? 'Archivo', style: AppText.small.copyWith(color: AppColors.primary))),
                              ]),
                            ),
                          )).toList(),
                    ),
                  ),
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.estado});
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
      case 'PDF_SUBIDO':
        return ('Subido', AppColors.primary);
      case 'PENDIENTE_BACKOFFICE':
      case 'EN_PROCESO':
        return ('En proceso', AppColors.accent);
      case 'OBSERVADO':
        return ('Observado', AppColors.warning);
      case 'COMPLETADO':
        return ('Completado', AppColors.success);
      case 'CANCELADO':
        return ('Cancelado', AppColors.danger);
      default:
        return (e ?? '—', AppColors.muted);
    }
  }
}
