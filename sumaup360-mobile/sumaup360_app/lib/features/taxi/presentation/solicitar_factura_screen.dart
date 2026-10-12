import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/primary_button.dart';
import '../application/taxi_providers.dart';
import '../domain/receipt_request.dart';

/// Formulario para solicitar o editar factura corporativa hacia la empresa.
class SolicitarFacturaScreen extends ConsumerStatefulWidget {
  const SolicitarFacturaScreen({
    super.key,
    required this.empresa,
    this.initialRequest,
  });

  final String empresa;
  final ReceiptRequestModel? initialRequest;

  @override
  ConsumerState<SolicitarFacturaScreen> createState() => _SolicitarFacturaScreenState();
}

class _SolicitarFacturaScreenState extends ConsumerState<SolicitarFacturaScreen> {
  final _formKey = GlobalKey<FormState>();

  final _monto = TextEditingController();
  final _ruc = TextEditingController();
  final _razonSocial = TextEditingController();
  final _whatsapp = TextEditingController();
  final _email = TextEditingController();
  final _observacion = TextEditingController();

  final _rucFocusNode = FocusNode();

  bool _loading = false;
  bool _lookingUp = false;
  bool _done = false;
  String? _error;

  bool get _isEditing => widget.initialRequest != null;

  @override
  void initState() {
    super.initState();
    _rucFocusNode.addListener(_onRucFocusChange);
    if (_isEditing) {
      final req = widget.initialRequest!;
      if (req.montoFinal > 0) {
        _monto.text = req.montoFinal.toStringAsFixed(2);
      }
      _ruc.text = req.docNumber ?? '';
      _razonSocial.text = req.customerName ?? '';
      _whatsapp.text = req.whatsapp ?? '';
      _email.text = req.email ?? '';
      _observacion.text = req.observacion ?? '';
    }
  }

  @override
  void dispose() {
    _rucFocusNode.removeListener(_onRucFocusChange);
    _rucFocusNode.dispose();
    _monto.dispose();
    _ruc.dispose();
    _razonSocial.dispose();
    _whatsapp.dispose();
    _email.dispose();
    _observacion.dispose();
    super.dispose();
  }

  void _onRucFocusChange() {
    if (!_rucFocusNode.hasFocus) {
      _lookupCustomer(_ruc.text);
    }
  }

  Future<void> _lookupCustomer(String doc) async {
    final d = doc.trim();
    if (d.length != 11 || !RegExp(r'^\d+$').hasMatch(d)) return;
    if (_razonSocial.text.trim().isNotEmpty) return; // Ya tiene razón social escrita

    setState(() => _lookingUp = true);
    try {
      final token = ref.read(qrTokenProvider).valueOrNull;
      final name = await ref.read(taxiRepositoryProvider).lookupCustomer(token: token, doc: d);
      if (name != null && name.trim().isNotEmpty && mounted) {
        setState(() {
          _razonSocial.text = name.trim();
        });
      }
    } finally {
      if (mounted) setState(() => _lookingUp = false);
    }
  }

  String? _validate() {
    final montoNum = double.tryParse(_monto.text.replaceAll(',', '.'));
    if (montoNum == null || montoNum <= 0) return 'Ingresa un monto valido mayor a 0.';

    final ruc = _ruc.text.trim();
    if (ruc.isEmpty) return 'Ingresa el numero de RUC.';
    if (!RegExp(r'^\d+$').hasMatch(ruc)) return 'El RUC debe ser numerico.';
    if (ruc.length != 11) return 'El RUC debe tener exactamente 11 digitos.';

    if (_razonSocial.text.trim().isEmpty) return 'Ingresa la razon social.';

    final wa = _whatsapp.text.replaceAll(RegExp(r'\D'), '');
    if (wa.length < 6) return 'Ingresa un WhatsApp valido.';

    return null;
  }

  Future<void> _submit() async {
    setState(() => _error = null);

    final validationError = _validate();
    if (validationError != null) {
      setState(() => _error = validationError);
      return;
    }

    setState(() => _loading = true);
    try {
      final montoNum = double.parse(_monto.text.replaceAll(',', '.'));

      if (_isEditing) {
        await ref.read(taxiRepositoryProvider).updateCorporateRequest(
              id: widget.initialRequest!.id,
              monto: montoNum,
              docType: 'RUC',
              docNumber: _ruc.text.trim(),
              customerName: _razonSocial.text.trim(),
              whatsapp: _whatsapp.text.trim(),
              email: _email.text.trim().isEmpty ? null : _email.text.trim(),
              observacion: _observacion.text.trim().isEmpty ? null : _observacion.text.trim(),
            );
      } else {
        await ref.read(taxiRepositoryProvider).createCorporateRequest(
              monto: montoNum,
              docType: 'RUC',
              docNumber: _ruc.text.trim(),
              customerName: _razonSocial.text.trim(),
              whatsapp: _whatsapp.text.trim(),
              email: _email.text.trim().isEmpty ? null : _email.text.trim(),
              observacion: _observacion.text.trim().isEmpty ? null : _observacion.text.trim(),
            );
      }

      ref.invalidate(taxiRequestsProvider);
      ref.invalidate(corporateRequestsProvider);
      ref.invalidate(corporateRidesProvider);

      if (mounted) {
        setState(() => _done = true);
      }
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No pudimos procesar tu solicitud. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(
        title: Text(_isEditing ? 'Editar solicitud' : 'Solicitar factura'),
      ),
      body: SafeArea(
        child: _done ? _buildSuccessView() : _buildFormView(),
      ),
    );
  }

  Widget _buildSuccessView() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.xl),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(AppSpacing.xxl),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
            boxShadow: AppShadows.card,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: AppColors.success.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                alignment: Alignment.center,
                child: const AppIcon('check', size: 32, color: AppColors.success),
              ),
              const SizedBox(height: AppSpacing.lg),
              Text(
                _isEditing ? 'Cambios guardados' : 'Solicitud enviada',
                style: AppText.h2,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                _isEditing
                    ? 'Los datos fiscales de tu solicitud hacia ${widget.empresa} han sido actualizados con exito.'
                    : 'Tu solicitud de factura hacia ${widget.empresa} ha sido registrada con exito. '
                        'Recibiras tu comprobante por WhatsApp o correo en un plazo maximo de 24 horas.',
                style: AppText.body.copyWith(color: AppColors.body),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppSpacing.xxl),
              PrimaryButton(
                label: 'Volver a Corporativo',
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFormView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Banner de contexto
            Container(
              padding: const EdgeInsets.all(AppSpacing.lg),
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
                border: Border.all(color: AppColors.primary.withValues(alpha: 0.2)),
              ),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: AppColors.surface,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    alignment: Alignment.center,
                    child: const AppIcon('receipt-tax', size: 24, color: AppColors.primary),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _isEditing ? 'Editar factura corporativa' : 'Factura corporativa',
                          style: AppText.bodyStrong.copyWith(color: AppColors.primaryDark),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          _isEditing
                              ? 'Modifica los datos fiscales antes de que la central emita el comprobante.'
                              : 'Dirigida a ${widget.empresa}. Completa los datos fiscales.',
                          style: AppText.small.copyWith(color: AppColors.primaryDark),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xl),

            // Tarjeta del formulario
            Container(
              padding: const EdgeInsets.all(AppSpacing.xl),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
                boxShadow: AppShadows.card,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Monto
                  AppTextField(
                    label: 'Monto a facturar (S/)',
                    controller: _monto,
                    hint: '0.00',
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    prefixIcon: 'moneda-sol-peruano',
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // RUC
                  AppTextField(
                    label: 'RUC',
                    controller: _ruc,
                    hint: '11 digitos',
                    maxLength: 11,
                    keyboardType: TextInputType.number,
                    prefixIcon: 'id',
                    suffix: _lookingUp
                        ? const Padding(
                            padding: EdgeInsets.all(12),
                            child: SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
                            ),
                          )
                        : null,
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // Razón Social
                  AppTextField(
                    label: 'Razon social',
                    controller: _razonSocial,
                    hint: 'Nombre o razon social de la empresa',
                    prefixIcon: 'building',
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // WhatsApp
                  AppTextField(
                    label: 'WhatsApp de contacto',
                    controller: _whatsapp,
                    hint: '999 999 999',
                    keyboardType: TextInputType.phone,
                    prefixIcon: 'phone',
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // Correo (opcional)
                  AppTextField(
                    label: 'Correo (opcional)',
                    controller: _email,
                    hint: 'contacto@empresa.com',
                    keyboardType: TextInputType.emailAddress,
                    prefixIcon: 'mail',
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // Observación (opcional)
                  AppTextField(
                    label: 'Observacion (opcional)',
                    controller: _observacion,
                    hint: 'Detalle de la carrera o servicio',
                    prefixIcon: 'notes',
                  ),

                  if (_error != null) ...[
                    const SizedBox(height: AppSpacing.md),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppColors.danger.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(AppSpacing.radius),
                      ),
                      child: Row(
                        children: [
                          const AppIcon('exclamation-circle', size: 16, color: AppColors.danger),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _error!,
                              style: const TextStyle(color: AppColors.danger, fontSize: 13, fontWeight: FontWeight.w500),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],

                  const SizedBox(height: AppSpacing.xl),

                  // Botón enviar
                  PrimaryButton(
                    label: _isEditing ? 'Guardar cambios' : 'Solicitar factura',
                    loading: _loading,
                    onPressed: _submit,
                  ),

                  const SizedBox(height: AppSpacing.sm),
                  const Center(
                    child: Text(
                      'Plazo maximo de atencion: 24 horas.',
                      style: TextStyle(fontSize: 12, color: AppColors.muted),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
