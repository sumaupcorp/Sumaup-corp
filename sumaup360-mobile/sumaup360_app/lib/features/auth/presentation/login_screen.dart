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
import '../../profile/application/profile_providers.dart';
import '../../registration/application/registration_controller.dart';
import '../application/auth_providers.dart';
import 'widgets/auth_decor.dart';

/// Inicio de sesion con correo/contrasena. Diseno: assets/mockups/screen/login.png.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _obscure = true;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await ref.read(authControllerProvider).signInWithEmail(_email.text, _password.text);
      if (!mounted) return;
      final verified = ref.read(authControllerProvider).isEmailVerified;
      if (!verified) {
        context.go(Routes.registerActivate);
        return;
      }
      // Si dejo el onboarding a medias, lo retoma en el paso donde se quedo.
      var dest = Routes.home;
      try {
        final p = await ref.read(profileRepositoryProvider).getMe();
        if (!p.onboardingCompleted) dest = registrationResumeRoute(p);
      } catch (_) {}
      if (!mounted) return;
      context.go(dest);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _google() async {
    final router = GoRouter.of(context);
    try {
      await ref.read(authControllerProvider).signInWithGoogle();
      // Cuenta nueva con Google: onboarding donde se quedo; existente: home.
      var dest = Routes.home;
      try {
        final p = await ref.read(profileRepositoryProvider).getMe();
        if (!p.onboardingCompleted) dest = registrationResumeRoute(p);
      } catch (_) {}
      router.go(dest);
    } on AppException catch (e) {
      setState(() => _error = e.message);
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
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        '¡Bienvenido!',
                        style: TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 26,
                          fontWeight: FontWeight.w700,
                          color: AppColors.onboardingTitle,
                          letterSpacing: 26 * -0.02,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Nos alegra verte.',
                        style: TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 14.5,
                          fontWeight: FontWeight.w400,
                          color: AppColors.authSubtitle,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      AuthField(
                        label: 'Correo',
                        controller: _email,
                        hint: 'Ingresa tu correo',
                        keyboardType: TextInputType.emailAddress,
                        validator: Validators.email,
                      ),
                      const SizedBox(height: AppSpacing.lg),
                      AuthField(
                        label: 'Contraseña',
                        controller: _password,
                        hint: 'Ingresa contraseña',
                        obscure: _obscure,
                        onToggleObscure: () => setState(() => _obscure = !_obscure),
                        validator: Validators.password,
                        labelTrailing: GestureDetector(
                          onTap: () => context.push(Routes.forgot),
                          child: const Text(
                            '¿Olvidaste tu contraseña?',
                            style: TextStyle(
                              fontFamily: 'Poppins',
                              fontSize: 13,
                              fontWeight: FontWeight.w400,
                              color: AppColors.onboardingTitle,
                            ),
                          ),
                        ),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: AppSpacing.md),
                        Text(_error!, style: const TextStyle(color: AppColors.danger)),
                      ],
                      const SizedBox(height: AppSpacing.xl),
                      AuthPrimaryButton(label: 'Iniciar sesión', loading: _loading, onPressed: _submit),
                      const SizedBox(height: AppSpacing.xl),
                      const AuthOrDivider(text: 'O inicia sesión con'),
                      const SizedBox(height: AppSpacing.xl),
                      AuthSocialButton(
                        icon: AppAssets.icGoogle,
                        label: 'Iniciar sesión con Google',
                        onPressed: _google,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      AuthSocialButton(
                        icon: AppAssets.icApple,
                        label: 'Iniciar sesión con Apple',
                        onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Apple estará disponible pronto.')),
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xxxl + AppSpacing.xxl),
                      AuthBottomLink(
                        question: '¿No tienes cuenta?',
                        action: 'Regístrate',
                        onTap: () => context.push(Routes.register),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
