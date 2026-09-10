import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../auth/application/auth_providers.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../notifications/application/push_service.dart';
import 'widgets/register_decor.dart';

/// Activacion de cuenta por enlace al correo (solo registro con correo;
/// Google/Apple saltan directo al nombre). Diseno: activar-cuenta.png.
class ActivateAccountScreen extends ConsumerStatefulWidget {
  const ActivateAccountScreen({super.key});

  @override
  ConsumerState<ActivateAccountScreen> createState() => _ActivateAccountScreenState();
}

class _ActivateAccountScreenState extends ConsumerState<ActivateAccountScreen> {
  bool _checking = false;
  String? _info;
  bool _infoIsError = false;

  Future<void> _check() async {
    setState(() {
      _checking = true;
      _info = null;
    });
    final verified = await ref.read(authControllerProvider).reloadAndCheckVerified();
    if (!mounted) return;
    setState(() => _checking = false);
    if (verified) {
      context.go(Routes.registerName);
    } else {
      setState(() {
        _info = 'Aún no detectamos la verificación. Abre el enlace de tu correo y vuelve a intentar.';
        _infoIsError = true;
      });
    }
  }

  Future<void> _resend() async {
    await ref.read(authControllerProvider).resendVerification();
    if (!mounted) return;
    setState(() {
      _info = 'Te reenviamos el enlace de activación.';
      _infoIsError = false;
    });
  }

  Future<void> _changeAccount() async {
    // "¿No es tu correo? Cambiar": cierra la cuenta recien creada y regresa al registro.
    await ref.read(pushServiceProvider).stop();
    await ref.read(authControllerProvider).signOut();
    if (mounted) context.go(Routes.register);
  }

  @override
  Widget build(BuildContext context) {
    final email = ref.read(authControllerProvider).currentUser?.email ?? 'tu correo';
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SingleChildScrollView(
          child: Column(
            children: [
              RegisterHeader(onBack: _changeAccount),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  AppSpacing.xl,
                  AppSpacing.lg,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icEmailConfirmacion)),
                    const SizedBox(height: AppSpacing.xl),
                    const Text('Activa tu cuenta', textAlign: TextAlign.center, style: RegisterText.titleCentered),
                    const SizedBox(height: AppSpacing.md),
                    Text.rich(
                      TextSpan(
                        text: 'Enviamos un enlace seguro a tu correo ',
                        children: [
                          TextSpan(
                            text: email,
                            style: const TextStyle(
                              fontWeight: FontWeight.w600,
                              color: AppColors.onboardingTitle,
                            ),
                          ),
                          const TextSpan(text: ' para confirmar que eres tú.'),
                        ],
                      ),
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    const Text(
                      'Abre el enlace desde tu bandeja de entrada y luego vuelve a la app para continuar.',
                      textAlign: TextAlign.center,
                      style: RegisterText.small,
                    ),
                    if (_info != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      Text(
                        _info!,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 13,
                          color: _infoIsError ? AppColors.danger : AppColors.loginWave,
                          height: 1.4,
                        ),
                      ),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(label: 'Ya verifiqué mi correo', loading: _checking, onPressed: _check),
                    const SizedBox(height: AppSpacing.lg),
                    RegisterOutlineButton(label: 'Reenviar enlace', onPressed: _resend),
                    const SizedBox(height: AppSpacing.xxxl * 2),
                    AuthBottomLink(
                      question: '¿No es tu correo?',
                      action: 'Cambiar',
                      onTap: _changeAccount,
                    ),
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
