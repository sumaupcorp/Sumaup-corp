import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import 'widgets/auth_decor.dart';

/// Confirmacion de envio del enlace de recuperacion.
/// Diseno: assets/mockups/screen/confirmar-email.png.
class CheckEmailScreen extends StatelessWidget {
  const CheckEmailScreen({super.key, required this.email});

  /// Correo al que se envio el enlace de recuperacion.
  final String email;

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AuthHeader(onBack: () => context.pop()),
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
                    const Center(child: AuthIconCircle(asset: AppAssets.icEmailConfirmacion)),
                    const SizedBox(height: AppSpacing.xl),
                    const Text(
                      'Revisa tu correo',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Poppins',
                        fontSize: 24,
                        fontWeight: FontWeight.w700,
                        color: AppColors.onboardingTitle,
                        letterSpacing: 24 * -0.02,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    Text.rich(
                      TextSpan(
                        text: 'Te enviamos un enlace para recuperar tu contraseña ',
                        children: [
                          TextSpan(
                            text: email,
                            style: const TextStyle(
                              fontWeight: FontWeight.w600,
                              color: AppColors.onboardingTitle,
                            ),
                          ),
                          const TextSpan(
                            text: '. Revisa tu bandeja de entrada y sigue las '
                                'instrucciones para crear una nueva contraseña.',
                          ),
                        ],
                      ),
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontFamily: 'Poppins',
                        fontSize: 14.5,
                        fontWeight: FontWeight.w400,
                        color: AppColors.authSubtitle,
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    const Text(
                      'Si no encuentras el correo, revisa la carpeta de spam o correo no deseado.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Poppins',
                        fontSize: 12,
                        fontWeight: FontWeight.w400,
                        color: AppColors.authHint,
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(
                      label: 'Entendido',
                      onPressed: () => context.go(Routes.login),
                    ),
                    const SizedBox(height: AppSpacing.xxxl * 3),
                    AuthBottomLink(
                      question: '¿No tienes cuenta?',
                      action: 'Regístrate',
                      onTap: () => context.push(Routes.register),
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
