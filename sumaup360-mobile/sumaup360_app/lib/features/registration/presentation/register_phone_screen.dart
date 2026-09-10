import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/phone_field.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Numero de telefono, opcional (registro-3.png, selector de pais con bandera).
class RegisterPhoneScreen extends ConsumerStatefulWidget {
  const RegisterPhoneScreen({super.key});

  @override
  ConsumerState<RegisterPhoneScreen> createState() => _RegisterPhoneScreenState();
}

class _RegisterPhoneScreenState extends ConsumerState<RegisterPhoneScreen> {
  final _phone = TextEditingController();
  Country _country = kCountries.first; // PE +51 por defecto
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _phone.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    if (digits.isEmpty) {
      setState(() => _error = 'Ingresa tu número o usa Omitir.');
      return;
    }
    if (digits.length < 6) {
      setState(() => _error = 'El número ingresado no parece válido.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(profileRepositoryProvider).updateMe(phone: digits, countryCode: _country.dial);
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.push(Routes.registerNotifications);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo guardar tu número. Intenta de nuevo.');
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
              RegisterHeader(onBack: () => context.backOr(Routes.registerPhoto)),
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
                    const Text('Introduce tu número (opcional)', style: RegisterText.title),
                    const SizedBox(height: 6),
                    const Text(
                      'Úsalo para proteger tu cuenta y recuperar el acceso cuando lo necesites.',
                      style: RegisterText.body,
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    PhoneField(
                      key: ValueKey(_country.iso),
                      controller: _phone,
                      initialIso: _country.iso,
                      onCountryChanged: (c) => _country = c,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(label: 'Continuar', loading: _loading, onPressed: _submit),
                    const SizedBox(height: AppSpacing.md),
                    RegisterTextButton(
                      label: 'Omitir',
                      onPressed: () => context.push(Routes.registerNotifications),
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
