import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/suma_image.dart';
import '../data/ai_diagnosis_repository.dart';

/// Diagnostico tributario con IA. Gratis, de un solo intento por cuenta.
/// Flujo: bienvenida + ingresa DNI/RUC -> consulta SUNAT + IA -> recomendacion -> chat.
class AiDiagnosisScreen extends ConsumerStatefulWidget {
  const AiDiagnosisScreen({super.key});

  @override
  ConsumerState<AiDiagnosisScreen> createState() => _AiDiagnosisScreenState();
}

class _AiDiagnosisScreenState extends ConsumerState<AiDiagnosisScreen> {
  final _doc = TextEditingController();
  String _docType = 'DNI';
  bool _generating = false;

  @override
  void dispose() {
    _doc.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    final doc = _doc.text.trim();
    if (doc.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ingresa tu DNI o RUC.')));
      return;
    }
    setState(() => _generating = true);
    try {
      await ref.read(aiDiagnosisRepositoryProvider).generate(doc: doc, docType: _docType);
      ref.invalidate(aiDiagnosisStatusProvider);
    } on AppException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo generar el diagnostico.')));
      }
    } finally {
      if (mounted) setState(() => _generating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final status = ref.watch(aiDiagnosisStatusProvider);
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(backgroundColor: AppColors.surfaceAlt, title: const Text('Diagnostico')),
      body: SafeArea(
        child: status.when(
          loading: () => const Center(child: CircularProgressIndicator(color: AppColors.primary)),
          error: (_, __) => _inputView(), // si no se pudo leer el estado, deja intentar
          data: (s) => s.latest != null ? _resultView(s.latest!) : _inputView(),
        ),
      ),
    );
  }

  // --- Bienvenida + ingreso de documento ---
  Widget _inputView() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
      children: [
        const Row(
          children: [
            SumaImage(AppAssets.sumaRecomendando, height: 64),
            SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Tu diagnostico tributario', style: AppText.h2),
                  SizedBox(height: 4),
                  Text('Gratis, una vez por cuenta.', style: AppText.small),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.lg),
        Container(
          padding: const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Ingresa tu DNI o RUC. Consultamos tu estado en SUNAT y Suma te dira si tu regimen '
                'es el adecuado o si te conviene cambiar para pagar menos impuestos.',
                style: AppText.body,
              ),
              const SizedBox(height: AppSpacing.lg),
              Row(
                children: [
                  _DocTypeChip(label: 'DNI', selected: _docType == 'DNI', onTap: () => setState(() => _docType = 'DNI')),
                  const SizedBox(width: AppSpacing.sm),
                  _DocTypeChip(label: 'RUC', selected: _docType == 'RUC', onTap: () => setState(() => _docType = 'RUC')),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              TextField(
                controller: _doc,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  hintText: _docType == 'RUC' ? '11 digitos' : '8 digitos',
                  filled: true,
                  fillColor: AppColors.surfaceAlt,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radius),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.lg),
              SizedBox(
                height: 52,
                child: ElevatedButton(
                  onPressed: _generating ? null : _generate,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                  ),
                  child: _generating
                      ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                      : const Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                          AppIcon('magic', size: 20, color: Colors.white),
                          SizedBox(width: 8),
                          Text('Generar mi diagnostico', style: AppText.button),
                        ]),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // --- Resultado (ya generado) ---
  Widget _resultView(DiagnosisReport report) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
      children: [
        const Row(
          children: [
            SumaImage(AppAssets.sumaRecomendando, height: 56),
            SizedBox(width: AppSpacing.md),
            Expanded(child: Text('Tu diagnostico esta listo', style: AppText.h2)),
          ],
        ),
        const SizedBox(height: AppSpacing.lg),
        Container(
          padding: const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if ((report.createdAt ?? '').isNotEmpty)
                Text(report.createdAt!.split('T').first, style: AppText.small),
              const SizedBox(height: 6),
              Text(report.resultado, style: AppText.body),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        SizedBox(
          height: 52,
          child: ElevatedButton(
            onPressed: () => context.push(Routes.chat),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
            ),
            child: const Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              AppIcon('help-small', size: 20, color: Colors.white),
              SizedBox(width: 8),
              Text('Tengo mas dudas, ir al chat', style: AppText.button),
            ]),
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        const Text(
          'Ya usaste tu diagnostico gratuito. Si cambiaste de RUC o necesitas otro, escribe a soporte.',
          textAlign: TextAlign.center, style: AppText.small,
        ),
      ],
    );
  }
}

class _DocTypeChip extends StatelessWidget {
  const _DocTypeChip({required this.label, required this.selected, required this.onTap});
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm + 2),
        decoration: BoxDecoration(
          color: selected ? AppColors.primarySoft : AppColors.surfaceAlt,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          border: Border.all(color: selected ? AppColors.primary : AppColors.border, width: selected ? 2 : 1),
        ),
        child: Text(label,
            style: AppText.bodyStrong.copyWith(color: selected ? AppColors.primary : AppColors.ink)),
      ),
    );
  }
}
