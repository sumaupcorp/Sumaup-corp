import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import '../application/registration_controller.dart';
import '../data/orientation_repository.dart';
import 'widgets/register_decor.dart';

/// Resultado de la orientacion tributaria (fin-sub-1.png) con sustentacion
/// real: recomendacion, costo estimado, por que le conviene, limites a tener
/// en cuenta, pasos para inscribirse y alternativa si su situacion cambia.
class OrientationResultScreen extends ConsumerStatefulWidget {
  const OrientationResultScreen({super.key});

  @override
  ConsumerState<OrientationResultScreen> createState() => _OrientationResultScreenState();
}

class _OrientationResultScreenState extends ConsumerState<OrientationResultScreen> {
  bool _loading = false;
  String? _error;

  Future<void> _goHome() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(profileRepositoryProvider).updateMe(onboardingCompleted: true);
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.go(Routes.home);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo completar. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Recien completado: usa el resultado en memoria. Abierto desde el home:
    // carga el resultado GUARDADO en el backend (no se vuelve a analizar).
    final draft = ref.watch(registrationControllerProvider).result;
    if (draft != null) return _content(context, draft);
    final saved = ref.watch(orientationStatusProvider);
    return saved.when(
      loading: () => _statusScaffold(
        const Center(child: CircularProgressIndicator(color: AppColors.loginWave)),
      ),
      error: (_, __) => _statusScaffold(
        Padding(
          padding: const EdgeInsets.all(AppSpacing.xl),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const AuthErrorText(message: 'No se pudo cargar tu orientación. Revisa tu conexión.'),
              const SizedBox(height: AppSpacing.xl),
              AuthPrimaryButton(
                label: 'Reintentar',
                onPressed: () => ref.invalidate(orientationStatusProvider),
              ),
            ],
          ),
        ),
      ),
      data: (s) => _content(context, s.result),
    );
  }

  Widget _statusScaffold(Widget body) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          children: [
            RegisterHeader(onBack: () => context.backOr(Routes.home)),
            Expanded(child: body),
          ],
        ),
      ),
    );
  }

  Widget _content(BuildContext context, OrientationResult? result) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SingleChildScrollView(
          child: Column(
            children: [
              const RegisterHeader(),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  AppSpacing.xl,
                  0,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icCheckSeccion)),
                    const SizedBox(height: AppSpacing.xl),
                    const Text(
                      '¡Resultado de tu orientación!',
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    Text.rich(
                      TextSpan(
                        text: 'Según tus respuestas, podrías evaluar inscribirte como ',
                        children: [
                          TextSpan(
                            text: result?.headline ?? '',
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              color: AppColors.ink,
                            ),
                          ),
                          const TextSpan(text: ' en el '),
                          TextSpan(
                            text: '${result?.regime ?? ''}.',
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              color: AppColors.ink,
                            ),
                          ),
                        ],
                      ),
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    if (result != null) ...[
                      if (result.estimatedCost != null) ...[
                        const SizedBox(height: AppSpacing.xl),
                        _CostCard(cost: result.estimatedCost!),
                      ],
                      if (result.whyItFits.isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.xl),
                        const _SectionTitle('¿Por qué te conviene?'),
                        const SizedBox(height: AppSpacing.md),
                        for (final point in result.whyItFits) ...[
                          _WhyItFitsTile(point: point),
                          const SizedBox(height: AppSpacing.md),
                        ],
                      ],
                      if (result.considerations.isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.md),
                        _ConsiderationsCard(items: result.considerations),
                      ],
                      if (result.nextSteps.isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.xl),
                        const _SectionTitle('Tus siguientes pasos'),
                        const SizedBox(height: AppSpacing.md),
                        for (var i = 0; i < result.nextSteps.length; i++) ...[
                          _StepTile(number: i + 1, text: result.nextSteps[i]),
                          const SizedBox(height: AppSpacing.md),
                        ],
                      ],
                      if (result.alternative != null) ...[
                        const SizedBox(height: AppSpacing.md),
                        _AlternativeCard(alternative: result.alternative!),
                      ],
                      if ((result.disclaimer ?? '').isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.lg),
                        Text(
                          result.disclaimer!,
                          textAlign: TextAlign.center,
                          style: RegisterText.small,
                        ),
                      ],
                    ],
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(label: 'Ir al inicio', loading: _loading, onPressed: _goHome),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        fontFamily: 'Poppins',
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: AppColors.onboardingTitle,
      ),
    );
  }
}

/// Estimado de pago en el regimen recomendado (tarjeta azul suave).
class _CostCard extends StatelessWidget {
  const _CostCard({required this.cost});
  final OrientationCost cost;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.25)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            cost.label,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: AppColors.primaryDark,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            cost.amount,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 28,
              fontWeight: FontWeight.w700,
              color: AppColors.primaryDark,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            cost.note,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 12.5,
              fontWeight: FontWeight.w400,
              color: AppColors.primaryDark,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}

/// Punto de sustentacion: titulo en negrita + detalle con la regla tributaria.
class _WhyItFitsTile extends StatelessWidget {
  const _WhyItFitsTile({required this.point});
  final OrientationPoint point;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.optionCardBg,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Image.asset(AppAssets.icCheckSeccion, width: 20, height: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  point.title,
                  style: const TextStyle(
                    fontFamily: 'Poppins',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  point.detail,
                  style: const TextStyle(
                    fontFamily: 'Poppins',
                    fontSize: 13,
                    fontWeight: FontWeight.w400,
                    color: AppColors.authSubtitle,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Limites y alertas del regimen (tarjeta ambar "Ten en cuenta").
class _ConsiderationsCard extends StatelessWidget {
  const _ConsiderationsCard({required this.items});
  final List<String> items;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.warning.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.warning.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Ten en cuenta',
            style: TextStyle(
              fontFamily: 'Poppins',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: AppColors.warning,
            ),
          ),
          const SizedBox(height: 8),
          for (final item in items) ...[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '•  ',
                  style: TextStyle(fontFamily: 'Poppins', fontSize: 13, color: AppColors.warning),
                ),
                Expanded(
                  child: Text(
                    item,
                    style: const TextStyle(
                      fontFamily: 'Poppins',
                      fontSize: 13,
                      fontWeight: FontWeight.w400,
                      color: AppColors.body,
                      height: 1.5,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
          ],
        ],
      ),
    );
  }
}

/// Paso numerado para inscribirse.
class _StepTile extends StatelessWidget {
  const _StepTile({required this.number, required this.text});
  final int number;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 26,
          height: 26,
          decoration: const BoxDecoration(color: AppColors.loginWave, shape: BoxShape.circle),
          alignment: Alignment.center,
          child: Text(
            '$number',
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Colors.white,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Text(
              text,
              style: const TextStyle(
                fontFamily: 'Poppins',
                fontSize: 13.5,
                fontWeight: FontWeight.w400,
                color: AppColors.body,
                height: 1.5,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

/// Alternativa si su situacion cambia (plan B).
class _AlternativeCard extends StatelessWidget {
  const _AlternativeCard({required this.alternative});
  final OrientationAlternative alternative;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.authFieldBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Si tu situación cambia',
            style: TextStyle(
              fontFamily: 'Poppins',
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: AppColors.authHint,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            alternative.regime,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 14.5,
              fontWeight: FontWeight.w600,
              color: AppColors.ink,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            alternative.when,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 13,
              fontWeight: FontWeight.w400,
              color: AppColors.authSubtitle,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
