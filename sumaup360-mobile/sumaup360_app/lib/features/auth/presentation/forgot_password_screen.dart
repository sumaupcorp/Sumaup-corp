import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/validators.dart';
import '../application/auth_providers.dart';
import 'widgets/auth_decor.dart';

/// Recuperar contrasena. Diseno: assets/mockups/screen/recuperar-contraseña.png
/// y error-recuperar-contraseña.png (estado de correo invalido).
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _email = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final email = _email.text.trim();
    if (email.isEmpty) {
      setState(() => _error = 'Por favor, ingresa tu correo electrónico.');
      return;
    }
    if (Validators.email(email) != null) {
      setState(() => _error = 'Por favor, proporcione una dirección de correo electrónico válida.');
      return;
    }
    setState(() {
      _error = null;
      _loading = true;
    });
    try {
      await ref.read(authControllerProvider).sendPasswordReset(email);
      if (!mounted) return;
      context.push(Routes.forgotSent, extra: email);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
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
                    const Center(child: AuthIconCircle(asset: AppAssets.icCandado)),
                    const SizedBox(height: AppSpacing.xl),
                    const Text(
                      'Recuperar Contraseña',
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
                    const Text(
                      'Introduce tu correo electrónico registrado a continuación '
                      'para recibir instrucciones para restablecer la contraseña.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Poppins',
                        fontSize: 14.5,
                        fontWeight: FontWeight.w400,
                        color: AppColors.authSubtitle,
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    AuthField(
                      label: 'Correo',
                      controller: _email,
                      hint: 'Ingresa tu correo',
                      keyboardType: TextInputType.emailAddress,
                      hasError: _error != null,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.sm),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(label: 'Enviar correo', loading: _loading, onPressed: _submit),
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
