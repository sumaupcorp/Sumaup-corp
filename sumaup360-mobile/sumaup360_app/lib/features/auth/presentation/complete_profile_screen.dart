import 'dart:io';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/validators.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/phone_field.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../profile/application/profile_providers.dart';
import '../application/auth_providers.dart';

class CompleteProfileScreen extends ConsumerStatefulWidget {
  const CompleteProfileScreen({super.key});

  @override
  ConsumerState<CompleteProfileScreen> createState() => _CompleteProfileScreenState();
}

class _CompleteProfileScreenState extends ConsumerState<CompleteProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _referral = TextEditingController();
  final _dni = TextEditingController();
  final _picker = ImagePicker();
  Country _country = kCountries.first; // PE +51 por defecto
  String? _clientType; // TAXISTA | DELIVERY_PEYA
  bool _clientTypeLocked = false; // se elige una sola vez al crear la cuenta
  String? _photoUrl;
  bool _uploadingPhoto = false;
  bool _prefilled = false;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _referral.dispose();
    _dni.dispose();
    super.dispose();
  }

  void _prefill() {
    if (_prefilled) return;
    final p = ref.read(profileMeProvider).valueOrNull;
    if (p == null) return;
    _prefilled = true;
    if ((p.fullName ?? '').isNotEmpty) _name.text = p.fullName!;
    if ((p.phone ?? '').isNotEmpty) _phone.text = p.phone!;
    if ((p.dni ?? '').isNotEmpty) _dni.text = p.dni!;
    if ((p.photoUrl ?? '').isNotEmpty) _photoUrl = p.photoUrl;
    _clientType = p.clientType;
    // El tipo de cliente se elige una sola vez al crear la cuenta. Si ya lo tiene,
    // queda bloqueado: para cambiar de rubro debe escribir a soporte.
    _clientTypeLocked = (p.clientType ?? '').isNotEmpty;
    if (p.countryCode != null) {
      _country = kCountries.firstWhere((c) => c.dial == p.countryCode, orElse: () => kCountries.first);
    }
  }

  Future<void> _pickPhoto() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: AppSpacing.md),
            ListTile(
              leading: const AppIcon('camera', color: AppColors.primary),
              title: const Text('Tomar foto'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const AppIcon('photo', color: AppColors.primary),
              title: const Text('Elegir de la galeria'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
            const SizedBox(height: AppSpacing.sm),
          ],
        ),
      ),
    );
    if (source == null) return;
    try {
      final picked = await _picker.pickImage(source: source, maxWidth: 800, imageQuality: 85);
      if (picked == null) return;
      setState(() => _uploadingPhoto = true);
      final url = await ref.read(profileRepositoryProvider).uploadAvatar(File(picked.path));
      if (mounted) setState(() => _photoUrl = url);
    } on AppException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'No se pudo subir la foto.');
    } finally {
      if (mounted) setState(() => _uploadingPhoto = false);
    }
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    if (_clientType == null) {
      setState(() => _error = 'Elige tu tipo de cliente.');
      return;
    }
    setState(() => _loading = true);
    try {
      // Sincroniza nombre y foto en Firebase y persiste el perfil en el backend.
      final user = ref.read(authControllerProvider).currentUser;
      await user?.updateDisplayName(_name.text.trim());
      if (_photoUrl != null) await user?.updatePhotoURL(_photoUrl);
      final phoneDigits = _phone.text.replaceAll(RegExp(r'\D'), '');
      final updated = await ref.read(profileRepositoryProvider).updateMe(
            fullName: _name.text.trim(),
            phone: phoneDigits,
            countryCode: _country.dial,
            referralCode: _referral.text.trim().isEmpty ? null : _referral.text.trim(),
            photoUrl: _photoUrl,
            clientType: _clientType,
            dni: _dni.text.trim(),
          );
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      // El perfil solo se completa con el RUC validado en SUNAT (ACTIVO+HABIDO).
      // Si aun falta, lo llevamos a conectar/validar su SUNAT.
      context.go(updated.profileCompleted ? Routes.home : Routes.sunat);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (e) {
      setState(() => _error = 'No se pudo guardar el perfil.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final email = ref.read(authControllerProvider).currentUser?.email ?? '';
    ref.watch(profileMeProvider); // dispara la carga para prefill
    _prefill();

    return Scaffold(
      appBar: AppBar(title: const Text('Completar perfil')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: AppSpacing.screen,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(child: _AvatarPicker(photoUrl: _photoUrl, uploading: _uploadingPhoto, onTap: _pickPhoto)),
                const SizedBox(height: AppSpacing.xl),
                const Text('Soy', style: AppText.bodyStrong),
                const SizedBox(height: AppSpacing.sm),
                if (_clientTypeLocked)
                  _LockedClientType(value: _clientType)
                else
                  _ClientTypePicker(
                    value: _clientType,
                    onChanged: (v) => setState(() => _clientType = v),
                  ),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Nombre completo', controller: _name,
                    prefixIcon: 'id', validator: (v) => Validators.required(v, field: 'El nombre')),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'DNI', controller: _dni,
                    prefixIcon: 'user', keyboardType: TextInputType.number, maxLength: 8,
                    validator: (v) => Validators.required(v, field: 'El DNI')),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Correo electronico', enabled: false,
                    controller: TextEditingController(text: email), prefixIcon: 'mail'),
                const SizedBox(height: AppSpacing.lg),
                PhoneField(
                  key: ValueKey(_country.iso),
                  controller: _phone,
                  initialIso: _country.iso,
                  onCountryChanged: (c) => _country = c,
                  validator: (v) => Validators.required(v, field: 'El celular'),
                ),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Codigo de referido (opcional)', controller: _referral,
                    prefixIcon: 'gift'),
                if (_error != null) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(_error!, style: const TextStyle(color: AppColors.danger)),
                ],
                const SizedBox(height: AppSpacing.xxl),
                PrimaryButton(label: 'Guardar', loading: _loading, onPressed: _submit),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Selector de tipo de cliente (Taxista / Delivery Peya).
class _ClientTypePicker extends StatelessWidget {
  const _ClientTypePicker({required this.value, required this.onChanged});
  final String? value;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: _option('TAXISTA', 'Taxista', 'receipt-tax')),
        const SizedBox(width: AppSpacing.md),
        Expanded(child: _option('DELIVERY_PEYA', 'Delivery / Peya', 'arrow-bar-to-up')),
      ],
    );
  }

  Widget _option(String code, String label, String icon) {
    final selected = value == code;
    return GestureDetector(
      onTap: () => onChanged(code),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg, horizontal: AppSpacing.md),
        decoration: BoxDecoration(
          color: selected ? AppColors.primarySoft : AppColors.surfaceAlt,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          border: Border.all(
            color: selected ? AppColors.primary : AppColors.border,
            width: selected ? 2 : 1,
          ),
        ),
        child: Column(
          children: [
            AppIcon(icon, size: 26, color: selected ? AppColors.primary : AppColors.muted),
            const SizedBox(height: 6),
            Text(label,
                textAlign: TextAlign.center,
                style: AppText.bodyStrong.copyWith(
                    color: selected ? AppColors.primary : AppColors.ink)),
          ],
        ),
      ),
    );
  }
}

/// Muestra el tipo de cliente ya elegido, en modo lectura. El usuario no puede cambiarlo
/// (se define una sola vez al crear la cuenta); para cambiar de rubro debe contactar a soporte.
class _LockedClientType extends StatelessWidget {
  const _LockedClientType({required this.value});
  final String? value;

  @override
  Widget build(BuildContext context) {
    final isTaxista = value == 'TAXISTA';
    final label = isTaxista ? 'Taxista' : 'Delivery / Peya';
    final icon = isTaxista ? 'receipt-tax' : 'arrow-bar-to-up';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg, horizontal: AppSpacing.md),
          decoration: BoxDecoration(
            color: AppColors.surfaceAlt,
            borderRadius: BorderRadius.circular(AppSpacing.radius),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              AppIcon(icon, size: 24, color: AppColors.primary),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Text(label, style: AppText.bodyStrong.copyWith(color: AppColors.ink)),
              ),
              const AppIcon('lock', size: 18, color: AppColors.muted),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          'Tu tipo de cuenta ya esta configurado. Para cambiar de rubro escribe a soporte.',
          style: AppText.small.copyWith(color: AppColors.muted),
        ),
      ],
    );
  }
}

class _AvatarPicker extends StatelessWidget {
  const _AvatarPicker({required this.photoUrl, required this.uploading, required this.onTap});
  final String? photoUrl;
  final bool uploading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Container(
          width: 92,
          height: 92,
          decoration: const BoxDecoration(color: AppColors.primarySoft, shape: BoxShape.circle),
          clipBehavior: Clip.antiAlias,
          alignment: Alignment.center,
          child: uploading
              ? const CircularProgressIndicator(color: AppColors.primary)
              : (photoUrl != null && photoUrl!.isNotEmpty)
                  ? CachedNetworkImage(imageUrl: photoUrl!, width: 92, height: 92, fit: BoxFit.cover,
                      errorWidget: (_, __, ___) => const AppIcon('user', size: 44, color: AppColors.primary))
                  : const AppIcon('user', size: 44, color: AppColors.primary),
        ),
        Positioned(
          right: 0,
          bottom: 0,
          child: GestureDetector(
            onTap: uploading ? null : onTap,
            child: Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                color: AppColors.primary,
                shape: BoxShape.circle,
                border: Border.all(color: AppColors.surface, width: 2),
              ),
              alignment: Alignment.center,
              child: const AppIcon('camera', size: 16, color: Colors.white),
            ),
          ),
        ),
      ],
    );
  }
}
