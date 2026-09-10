import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Intro de la orientacion tributaria para quienes no tienen RUC
/// (registro-sub-1.png, icono ic-rayo.png). Omitir deja la orientacion
/// pendiente y va al home; Comenzar inicia el cuestionario.
class OrientationIntroScreen extends ConsumerStatefulWidget {
  const OrientationIntroScreen({super.key});

  @override
  ConsumerState<OrientationIntroScreen> createState() => _OrientationIntroScreenState();
}

class _OrientationIntroScreenState extends ConsumerState<OrientationIntroScreen> {
  bool _skipping = false;
  String? _error;

  Future<void> _skip() async {
    // Usuario con RUC que entro desde el home a reevaluarse: omitir no marca
    // nada pendiente, solo regresa.
    final status = ref.read(profileMeProvider).valueOrNull?.orientationStatus;
    if (status == 'NOT_REQUIRED') {
      context.backOr(Routes.home);
      return;
    }
    setState(() {
      _skipping = true;
      _error = null;
    });
    try {
      // Queda pendiente: el home mostrara el aviso para completarla despues.
      await ref.read(profileRepositoryProvider).updateMe(
            orientationStatus: 'PENDING',
            onboardingCompleted: true,
          );
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.go(Routes.home);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo continuar. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _skipping = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SingleChildScrollView(
          child: Column(
            children: [
              RegisterHeader(onBack: () => context.backOr(Routes.registerRucOption)),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  AppSpacing.xl,
                  AppSpacing.xxl,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icRayo)),
                    const SizedBox(height: AppSpacing.xxl),
                    const Text(
                      'Conozcamos mejor tu actividad',
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.md),
                    const Text(
                      'Responde unas preguntas breves para mostrarte una orientación tributaria acorde con tu situación.',
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xxl),
                    AuthPrimaryButton(
                      label: 'Comenzar',
                      onPressed: () => context.push(Routes.orientationQuiz),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    _skipping
                        ? const Center(
                            child: Padding(
                              padding: EdgeInsets.all(12),
                              child: SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              ),
                            ),
                          )
                        : RegisterTextButton(label: 'Omitir', onPressed: _skip),
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
