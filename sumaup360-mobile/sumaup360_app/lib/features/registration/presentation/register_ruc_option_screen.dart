import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_spacing.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import 'widgets/register_decor.dart';

/// Configuracion de datos tributarios (registro-6.png): tengo RUC / aun no.
class RegisterRucOptionScreen extends ConsumerStatefulWidget {
  const RegisterRucOptionScreen({super.key});

  @override
  ConsumerState<RegisterRucOptionScreen> createState() => _RegisterRucOptionScreenState();
}

class _RegisterRucOptionScreenState extends ConsumerState<RegisterRucOptionScreen> {
  bool? _hasRuc;

  void _submit() {
    final hasRuc = _hasRuc;
    if (hasRuc == null) return;
    context.push(hasRuc ? Routes.registerRuc : Routes.orientationIntro);
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
              RegisterHeader(onBack: () => context.backOr(Routes.registerActivity)),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text('Configura tus datos tributarios', style: RegisterText.title),
                      const SizedBox(height: 6),
                      const Text(
                        'Indícanos si cuentas con RUC para mostrarte las herramientas y obligaciones que correspondan a tu actividad.',
                        style: RegisterText.body,
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      RegisterOptionCard(
                        icon: AppAssets.icSunat,
                        title: 'Tengo RUC',
                        subtitle: 'Ya estoy inscrito en SUNAT y deseo vincular mis datos tributarios.',
                        selected: _hasRuc == true,
                        onTap: () => setState(() => _hasRuc = true),
                      ),
                      const SizedBox(height: AppSpacing.lg),
                      RegisterOptionCard(
                        icon: AppAssets.icNoRuc,
                        title: 'Aún no tengo RUC',
                        subtitle: 'Te ayudaremos a identificar qué opción tributaria podría corresponderte según tu actividad.',
                        selected: _hasRuc == false,
                        onTap: () => setState(() => _hasRuc = false),
                      ),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: _hasRuc != null
                    ? AuthPrimaryButton(label: 'Continuar', onPressed: _submit)
                    : const RegisterDisabledButton(label: 'Continuar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
