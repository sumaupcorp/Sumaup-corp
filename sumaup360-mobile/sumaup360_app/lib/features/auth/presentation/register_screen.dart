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

/// Registro con correo/contrasena. Diseno: assets/mockups/screen/register.png.
class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirm = true;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await ref.read(authControllerProvider).registerWithEmail(_email.text, _password.text);
      // Registro por correo: primero activa la cuenta con el enlace del email.
      if (mounted) context.go(Routes.registerActivate);
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
      // Google/Apple no requieren activar el correo: entran directo al
      // onboarding (retomando el paso donde se quedo). Si la cuenta ya
      // existia y completo el onboarding, va al home.
      var dest = Routes.registerName;
      try {
        final p = await ref.read(profileRepositoryProvider).getMe();
        dest = p.onboardingCompleted ? Routes.home : registrationResumeRoute(p);
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
                        'Crea tu cuenta',
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
                        '¡Únete a nosotros y\ncomienza tu aventura!',
                        style: TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 14.5,
                          fontWeight: FontWeight.w400,
                          color: AppColors.authSubtitle,
                          height: 1.4,
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
                        obscure: _obscurePassword,
                        onToggleObscure: () => setState(() => _obscurePassword = !_obscurePassword),
                        validator: Validators.password,
                      ),
                      const SizedBox(height: AppSpacing.lg),
                      AuthField(
                        label: 'Repetir Contraseña',
                        controller: _confirm,
                        hint: 'Ingresa contraseña',
                        obscure: _obscureConfirm,
                        onToggleObscure: () => setState(() => _obscureConfirm = !_obscureConfirm),
                        validator: (v) => Validators.confirm(v, _password.text),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: AppSpacing.md),
                        Text(_error!, style: const TextStyle(color: AppColors.danger)),
                      ],
                      const SizedBox(height: AppSpacing.xl),
                      AuthPrimaryButton(label: 'Crear cuenta', loading: _loading, onPressed: _submit),
                      const SizedBox(height: AppSpacing.xl),
                      const AuthOrDivider(text: 'O regístrate con'),
                      const SizedBox(height: AppSpacing.xl),
                      AuthSocialButton(
                        icon: AppAssets.icGoogle,
                        label: 'Regístrate con Google',
                        onPressed: _google,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      AuthSocialButton(
                        icon: AppAssets.icApple,
                        label: 'Regístrate con Apple',
                        onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Apple estará disponible pronto.')),
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      const _TermsText(),
                      const SizedBox(height: AppSpacing.xxl),
                      AuthBottomLink(
                        question: '¿Ya tienes una cuenta?',
                        action: 'Inicia sesión',
                        onTap: () => context.push(Routes.login),
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

class _TermsText extends StatelessWidget {
  const _TermsText();

  @override
  Widget build(BuildContext context) {
    const base = TextStyle(
      fontFamily: 'Poppins',
      fontSize: 12.5,
      fontWeight: FontWeight.w400,
      color: AppColors.onboardingTitle,
      height: 1.5,
    );
    const link = TextStyle(color: AppColors.authLink, fontWeight: FontWeight.w500);
    return const Text.rich(
      TextSpan(
        text: 'Al registrarte, aceptas nuestros ',
        style: base,
        children: [
          TextSpan(text: 'Términos y Condiciones', style: link),
          TextSpan(text: ' y nuestra '),
          TextSpan(text: 'Política de Privacidad', style: link),
          TextSpan(text: '.'),
        ],
      ),
      textAlign: TextAlign.center,
    );
  }
}
