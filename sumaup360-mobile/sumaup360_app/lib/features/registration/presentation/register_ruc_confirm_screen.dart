import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../fiscal/domain/ruc_fiscal.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Confirmacion de los datos de SUNAT (registro-sub-3.png). Solo lectura:
/// si el RUC no es el suyo, retrocede e ingresa otro.
class RegisterRucConfirmScreen extends ConsumerStatefulWidget {
  const RegisterRucConfirmScreen({super.key, required this.fiscal});
  final RucFiscal fiscal;

  @override
  ConsumerState<RegisterRucConfirmScreen> createState() => _RegisterRucConfirmScreenState();
}

class _RegisterRucConfirmScreenState extends ConsumerState<RegisterRucConfirmScreen> {
  bool _loading = false;
  String? _error;

  Future<void> _confirm() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // Con RUC vinculado la orientacion tributaria no es necesaria.
      await ref.read(profileRepositoryProvider).updateMe(orientationStatus: 'NOT_REQUIRED');
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.push(Routes.registerDone);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo confirmar. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final f = widget.fiscal;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SafeArea(
          top: false,
          child: Column(
            children: [
              RegisterHeader(onBack: () => context.backOr(Routes.registerRuc)),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text('Confirma tus datos', style: RegisterText.title),
                      const SizedBox(height: 6),
                      const Text(
                        'Revisamos la información vinculada a tu RUC para personalizar tu experiencia.',
                        style: RegisterText.body,
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      RegisterReadOnlyField(label: 'Nombre o razón social', value: f.razonSocial ?? ''),
                      const SizedBox(height: AppSpacing.lg),
                      RegisterReadOnlyField(label: 'Estado', value: f.taxStatus ?? ''),
                      const SizedBox(height: AppSpacing.lg),
                      RegisterReadOnlyField(label: 'Actividad económica', value: f.economicActivity ?? ''),
                      const SizedBox(height: AppSpacing.lg),
                      RegisterReadOnlyField(label: 'Ubicación', value: f.domicilioFiscal ?? ''),
                      if (_error != null) ...[
                        const SizedBox(height: AppSpacing.md),
                        AuthErrorText(message: _error!),
                      ],
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: AuthPrimaryButton(label: 'Continuar', loading: _loading, onPressed: _confirm),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
