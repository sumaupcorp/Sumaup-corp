import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../auth/application/auth_providers.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import 'widgets/register_decor.dart';

/// Foto de perfil, opcional (registro-2.png, icono ic-persona.png).
class RegisterPhotoScreen extends ConsumerStatefulWidget {
  const RegisterPhotoScreen({super.key});

  @override
  ConsumerState<RegisterPhotoScreen> createState() => _RegisterPhotoScreenState();
}

class _RegisterPhotoScreenState extends ConsumerState<RegisterPhotoScreen> {
  final _picker = ImagePicker();
  File? _photo;
  bool _loading = false;
  String? _error;

  Future<void> _pickPhoto() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: AppSpacing.md),
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined, color: AppColors.loginWave),
              title: const Text('Tomar foto', style: TextStyle(fontFamily: 'Poppins', fontSize: 15)),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_outlined, color: AppColors.loginWave),
              title: const Text('Elegir de la galería', style: TextStyle(fontFamily: 'Poppins', fontSize: 15)),
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
      setState(() {
        _photo = File(picked.path);
        _error = null;
      });
    } catch (_) {
      setState(() => _error = 'No se pudo cargar la imagen.');
    }
  }

  Future<void> _continueWithPhoto() async {
    final photo = _photo;
    if (photo == null) {
      await _pickPhoto();
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final url = await ref.read(profileRepositoryProvider).uploadAvatar(photo);
      await ref.read(authControllerProvider).currentUser?.updatePhotoURL(url);
      await ref.read(profileRepositoryProvider).updateMe(photoUrl: url);
      ref.invalidate(profileMeProvider);
      if (!mounted) return;
      context.push(Routes.registerPhone);
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo subir la foto. Intenta de nuevo.');
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
            children: [
              RegisterHeader(onBack: () => context.backOr(Routes.registerName)),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  AppSpacing.xl,
                  AppSpacing.xl,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(
                      child: Container(
                        width: 150,
                        height: 150,
                        decoration: const BoxDecoration(
                          color: AppColors.optionCardBg,
                          shape: BoxShape.circle,
                        ),
                        clipBehavior: Clip.antiAlias,
                        alignment: Alignment.center,
                        child: _photo != null
                            ? Image.file(_photo!, width: 150, height: 150, fit: BoxFit.cover)
                            : Image.asset(AppAssets.icPersona, width: 82, height: 82),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xxl),
                    const Text(
                      'Agrega una foto de perfil',
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.md),
                    const Text(
                      'Personaliza tu cuenta para identificarla fácilmente. Puedes hacerlo más adelante.',
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppSpacing.md),
                      AuthErrorText(message: _error!),
                    ],
                    const SizedBox(height: AppSpacing.xl),
                    AuthPrimaryButton(
                      label: _photo == null ? 'Agregar foto' : 'Continuar',
                      loading: _loading,
                      onPressed: _continueWithPhoto,
                    ),
                    const SizedBox(height: AppSpacing.md),
                    RegisterTextButton(
                      label: 'Omitir',
                      onPressed: () => context.push(Routes.registerPhone),
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
