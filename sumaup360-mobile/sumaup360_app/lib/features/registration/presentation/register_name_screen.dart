import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/application/auth_providers.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../notifications/application/push_service.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Nombre y apellido (registro-1.png). Si el usuario entro con Google/Apple
/// se autocompleta desde el displayName de Firebase; con correo se ingresa manual.
class RegisterNameScreen extends ConsumerStatefulWidget {
  const RegisterNameScreen({super.key});

  @override
  ConsumerState<RegisterNameScreen> createState() => _RegisterNameScreenState();
}

class _RegisterNameScreenState extends ConsumerState<RegisterNameScreen> {
  final _first = TextEditingController();
  final _last = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _prefillFromFirebase();
    _first.addListener(_onChanged);
    _last.addListener(_onChanged);
  }

  void _onChanged() => setState(() {});

  /// Google/Apple traen displayName ("Ramon Blandino"): primera palabra como
  /// nombre y el resto como apellido.
  void _prefillFromFirebase() {
    final displayName = ref.read(authControllerProvider).currentUser?.displayName ?? '';
    if (displayName.trim().isEmpty) return;
    final parts = displayName.trim().split(RegExp(r'\s+'));
    _first.text = parts.first;
    if (parts.length > 1) _last.text = parts.sublist(1).join(' ');
  }

  bool get _valid => _first.text.trim().isNotEmpty && _last.text.trim().isNotEmpty;

  /// La flecha de regreso en este primer paso funciona como cerrar sesion:
  /// permite salir del onboarding y entrar luego con otra cuenta.
  Future<void> _signOut() async {
    await ref.read(pushServiceProvider).stop();
    await ref.read(authControllerProvider).signOut();
    if (mounted) context.go(Routes.login);
  }

  @override
  void dispose() {
    _first.dispose();
    _last.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_valid || _loading) return;
    setState(() {
      _error = null;
      _loading = true;
    });
    final first = _first.text.trim();
    final last = _last.text.trim();
    try {
      await ref.read(authControllerProvider).currentUser?.updateDisplayName('$first $last');
      await ref.read(profileRepositoryProvider).updateMe(
            firstName: first,
            lastName: last,
            fullName: '$first $last',
          );
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.push(Routes.registerPhoto);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo guardar tu nombre. Intenta de nuevo.');
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
              RegisterHeader(onBack: _signOut),
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
                    const Text('¿Cuál es tu nombre?', style: RegisterText.title),
                    const SizedBox(height: 6),
                    const Text(
                      'Así personalizamos tu cuenta y tus comprobantes.',
                      style: RegisterText.body,
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    _NameField(first: _first, last: _last),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xxl),
                    _valid
                        ? AuthPrimaryButton(label: 'Continuar', loading: _loading, onPressed: _submit)
                        : const RegisterDisabledButton(label: 'Continuar'),
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

/// Campo unico con icono de persona y dos entradas: Primer nombre | Apellido.
class _NameField extends StatelessWidget {
  const _NameField({required this.first, required this.last});
  final TextEditingController first;
  final TextEditingController last;

  static const _textStyle = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 14.5,
    fontWeight: FontWeight.w400,
    color: AppColors.ink,
  );
  static const _hintStyle = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: AppColors.authHint,
  );

  /// Anula el InputDecorationTheme global (fondo + borde por campo) para que
  /// los dos TextField se vean como un solo input: solo el borde del contenedor.
  static const _innerDecoration = InputDecoration(
    hintStyle: _hintStyle,
    filled: false,
    border: InputBorder.none,
    enabledBorder: InputBorder.none,
    focusedBorder: InputBorder.none,
    contentPadding: EdgeInsets.symmetric(vertical: 16),
    isDense: true,
  );

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.authFieldBorder),
      ),
      child: Row(
        children: [
          Image.asset(AppAssets.icPersonInput, width: 24, height: 24),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: first,
              textCapitalization: TextCapitalization.words,
              style: _textStyle,
              decoration: _innerDecoration.copyWith(hintText: 'Primer nombre'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: last,
              textCapitalization: TextCapitalization.words,
              style: _textStyle,
              decoration: _innerDecoration.copyWith(hintText: 'Apellido'),
            ),
          ),
        ],
      ),
    );
  }
}
