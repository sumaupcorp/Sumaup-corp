import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/application/auth_providers.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Cierre del onboarding con check verde (registro-sub-4.png):
/// "¡Todo listo, {nombre}!" y boton para ir al inicio.
class RegisterDoneScreen extends ConsumerStatefulWidget {
  const RegisterDoneScreen({super.key});

  @override
  ConsumerState<RegisterDoneScreen> createState() => _RegisterDoneScreenState();
}

class _RegisterDoneScreenState extends ConsumerState<RegisterDoneScreen> {
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
    final user = ref.read(authControllerProvider).currentUser;
    final firstName = (user?.displayName ?? '').trim().split(RegExp(r'\s+')).first;
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
                  AppSpacing.xxl,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icCheckSeccion)),
                    const SizedBox(height: AppSpacing.xxl),
                    Text.rich(
                      TextSpan(
                        text: '¡Todo listo',
                        children: [
                          if (firstName.isNotEmpty)
                            TextSpan(
                              text: ', $firstName',
                              style: const TextStyle(fontWeight: FontWeight.w700),
                            ),
                          const TextSpan(text: '!'),
                        ],
                      ),
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    const Text(
                      'Tu cuenta y actividad ya están configuradas. Empieza a organizar tus ingresos, gastos y comprobantes desde un solo lugar.',
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xxl),
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
