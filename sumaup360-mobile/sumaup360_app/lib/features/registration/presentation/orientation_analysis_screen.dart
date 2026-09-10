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

/// Pantalla de carga mientras la IA analiza el perfil
/// (Analisis-sub-1-IA.png, icono ic-cohete.png).
class OrientationAnalysisScreen extends ConsumerStatefulWidget {
  const OrientationAnalysisScreen({super.key});

  @override
  ConsumerState<OrientationAnalysisScreen> createState() => _OrientationAnalysisScreenState();
}

class _OrientationAnalysisScreenState extends ConsumerState<OrientationAnalysisScreen> {
  String? _error;

  @override
  void initState() {
    super.initState();
    _analyze();
  }

  Future<void> _analyze() async {
    setState(() => _error = null);
    final draft = ref.read(registrationControllerProvider);
    final activity = draft.activity ??
        ref.read(profileMeProvider).valueOrNull?.clientType ??
        'TAXISTA';
    try {
      final result = await ref.read(orientationRepositoryProvider).analyze(
            activity: activity,
            answers: draft.completedAnswers,
          );
      ref.read(registrationControllerProvider.notifier).setResult(result);
      // El backend marco COMPLETED: refrescar perfil (tarjeta del home) y el
      // resultado guardado para futuras lecturas.
      ref.invalidate(profileMeProvider);
      ref.invalidate(orientationStatusProvider);
      if (!mounted) return;
      context.pushReplacement(Routes.orientationResult);
    } on AppException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'No pudimos completar el análisis. Intenta de nuevo.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          children: [
            RegisterHeader(onBack: () => context.backOr(Routes.orientationQuiz)),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xl),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icCohete)),
                    const SizedBox(height: AppSpacing.xxl),
                    const Text(
                      'Nuestra Inteligencia artificial está analizando tu perfil',
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.xxl),
                    if (_error == null)
                      const Center(
                        child: SizedBox(
                          width: 44,
                          height: 44,
                          child: CircularProgressIndicator(
                            strokeWidth: 4,
                            color: AppColors.loginWave,
                          ),
                        ),
                      )
                    else ...[
                      AuthErrorText(message: _error!),
                      const SizedBox(height: AppSpacing.xl),
                      AuthPrimaryButton(label: 'Reintentar', onPressed: _analyze),
                    ],
                    const SizedBox(height: AppSpacing.xxxl * 2),
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
