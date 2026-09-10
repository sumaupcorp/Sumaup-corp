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
import '../application/registration_controller.dart';
import 'widgets/register_decor.dart';

/// Seleccion de actividad (registro-5.png): taxista, repartidor o profesional.
class RegisterActivityScreen extends ConsumerStatefulWidget {
  const RegisterActivityScreen({super.key});

  @override
  ConsumerState<RegisterActivityScreen> createState() => _RegisterActivityScreenState();
}

class _RegisterActivityScreenState extends ConsumerState<RegisterActivityScreen> {
  String? _selected;
  bool _loading = false;
  String? _error;

  static const _options = [
    (
      code: 'TAXISTA',
      icon: AppAssets.icCarro,
      title: 'Taxista',
      subtitle: 'Controla viajes, gastos, comprobantes y solicitudes por QR.',
    ),
    (
      code: 'DELIVERY_PEYA',
      icon: AppAssets.icMoto,
      title: 'Repartidor / delivery',
      subtitle: 'Sube tus reportes o PDFs de plataforma y organiza tus ingresos.',
    ),
    (
      code: 'SERVICIOS_PROFESIONALES',
      icon: AppAssets.icProfesional,
      title: 'Servicios profesionales',
      subtitle: 'Gestiona recibos por honorarios, ingresos y retenciones.',
    ),
  ];

  Future<void> _submit() async {
    final selected = _selected;
    if (selected == null || _loading) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(profileRepositoryProvider).updateMe(clientType: selected);
      ref.read(registrationControllerProvider.notifier).setActivity(selected);
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.push(Routes.registerRucOption);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo guardar tu actividad. Intenta de nuevo.');
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
        body: SafeArea(
          top: false,
          child: Column(
            children: [
              RegisterHeader(onBack: () => context.backOr(Routes.registerNotifications)),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text('¿Qué actividad realizas?', style: RegisterText.title),
                      const SizedBox(height: 6),
                      const Text(
                        'Selecciona la opción que mejor describe tu actividad para personalizar tu experiencia.',
                        style: RegisterText.body,
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      for (final o in _options) ...[
                        RegisterOptionCard(
                          icon: o.icon,
                          title: o.title,
                          subtitle: o.subtitle,
                          selected: _selected == o.code,
                          onTap: () => setState(() => _selected = o.code),
                        ),
                        const SizedBox(height: AppSpacing.lg),
                      ],
                      if (_error != null) AuthErrorText(message: _error!),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: _selected != null
                    ? AuthPrimaryButton(label: 'Continuar', loading: _loading, onPressed: _submit)
                    : const RegisterDisabledButton(label: 'Continuar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
