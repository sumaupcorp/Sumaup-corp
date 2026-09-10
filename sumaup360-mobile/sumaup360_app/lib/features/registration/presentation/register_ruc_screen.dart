import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../fiscal/application/fiscal_providers.dart';
import 'widgets/register_decor.dart';

/// Ingreso del RUC (registro-sub-2.png). Busca la ficha en SUNAT via el
/// microservicio y navega a la confirmacion con los datos encontrados.
class RegisterRucScreen extends ConsumerStatefulWidget {
  const RegisterRucScreen({super.key});

  @override
  ConsumerState<RegisterRucScreen> createState() => _RegisterRucScreenState();
}

class _RegisterRucScreenState extends ConsumerState<RegisterRucScreen> {
  final _ruc = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _ruc.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _ruc.dispose();
    super.dispose();
  }

  bool get _valid => _ruc.text.trim().length == 11;

  Future<void> _search() async {
    final ruc = _ruc.text.trim();
    if (ruc.length != 11) {
      setState(() => _error = 'El RUC debe tener 11 dígitos.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // Consulta SUNAT (puede tardar: el scraper resuelve un captcha).
      final fiscal = await ref.read(fiscalRepositoryProvider).refreshRuc(ruc: ruc);
      if (!mounted) return;
      if (!fiscal.hasData) {
        setState(() => _error = 'No encontramos información para ese RUC. Verifica e intenta de nuevo.');
        return;
      }
      context.push(Routes.registerRucConfirm, extra: fiscal);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo consultar SUNAT. Intenta de nuevo.');
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
              RegisterHeader(onBack: () => context.backOr(Routes.registerRucOption)),
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
                    const Text('Datos tributarios', style: RegisterText.title),
                    const SizedBox(height: 6),
                    const Text(
                      'Necesitamos tu RUC para verificar tu información tributaria pública y configurar tu experiencia según tu actividad.',
                      style: RegisterText.body,
                    ),
                    const SizedBox(height: AppSpacing.xl),
                    TextField(
                      controller: _ruc,
                      keyboardType: TextInputType.number,
                      maxLength: 11,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      style: const TextStyle(
                        fontFamily: 'Poppins',
                        fontSize: 14.5,
                        color: AppColors.ink,
                      ),
                      decoration: InputDecoration(
                        hintText: 'Ingresa tu RUC',
                        counterText: '',
                        hintStyle: const TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 14,
                          color: AppColors.authHint,
                        ),
                        filled: true,
                        fillColor: Colors.white,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(
                            color: _error != null ? AppColors.danger : AppColors.authFieldBorder,
                          ),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide(
                            color: _error != null ? AppColors.danger : AppColors.loginWave,
                            width: 1.4,
                          ),
                        ),
                      ),
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.sm),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    if (_loading) ...[
                      const SizedBox(height: AppSpacing.sm),
                      const Center(child: CircularProgressIndicator(color: AppColors.loginWave)),
                      const SizedBox(height: AppSpacing.md),
                      const Text(
                        'Consultando tu ficha RUC en SUNAT, esto puede tardar unos segundos...',
                        textAlign: TextAlign.center,
                        style: RegisterText.small,
                      ),
                    ] else
                      _valid
                          ? AuthPrimaryButton(label: 'Continuar', onPressed: _search)
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
