import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../auth/application/auth_providers.dart';
import '../../notifications/application/push_service.dart';
import '../application/profile_providers.dart';

/// "Mi cuenta": perfil del usuario, accesos y cierre de sesion. Se llega desde el
/// avatar del Home (ya no es una pestana del bottom nav).
class AccountScreen extends ConsumerWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final email = ref.read(authControllerProvider).currentUser?.email ?? '';
    final profile = ref.watch(profileMeProvider).valueOrNull;
    final name = (profile?.fullName != null && profile!.fullName!.trim().isNotEmpty)
        ? profile.fullName!.trim()
        : (email.contains('@') ? email.split('@').first : 'Mi cuenta');

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(backgroundColor: AppColors.surfaceAlt, title: const Text('Mi cuenta')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
          children: [
            _card(
              child: Row(
                children: [
                  _avatar(profile?.photoUrl, name),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(name, style: AppText.title, maxLines: 1, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 2),
                        Text(email, style: AppText.small, maxLines: 1, overflow: TextOverflow.ellipsis),
                        if ((profile?.phone ?? '').isNotEmpty) ...[
                          const SizedBox(height: 2),
                          Text('${profile!.countryCode ?? ''} ${profile.phone}', style: AppText.small),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            _tile(context, 'user-edit', AppColors.success, 'Editar perfil', () => context.push(Routes.completeProfile)),
            _tile(context, 'lock', AppColors.primary, 'Conectar SUNAT (Clave SOL)', () => context.push(Routes.sunat)),
            _tile(context, 'crown', const Color(0xFFCA8A04), 'Mi plan', () => context.push(Routes.plans)),
            _tile(context, 'magic', const Color(0xFF7C3AED), 'Mi diagnostico', () => context.push(Routes.aiDiagnosis)),
            _tile(context, 'brand-google-analytics', AppColors.primary, 'Estadisticas', () => context.push(Routes.stats)),
            const SizedBox(height: AppSpacing.lg),
            _tile(context, 'logout', AppColors.danger, 'Cerrar sesion', () async {
              // Da de baja el token push antes de cerrar sesion (requiere idToken).
              await ref.read(pushServiceProvider).stop();
              await ref.read(authControllerProvider).signOut();
              if (context.mounted) context.go(Routes.auth);
            }, danger: true),
          ],
        ),
      ),
    );
  }

  Widget _card({required Widget child}) => Container(
        padding: const EdgeInsets.all(AppSpacing.lg),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          boxShadow: AppShadows.card,
        ),
        child: child,
      );

  Widget _avatar(String? photoUrl, String name) {
    final initials = name.isNotEmpty ? name.characters.first.toUpperCase() : 'U';
    if (photoUrl != null && photoUrl.isNotEmpty) {
      return ClipOval(
        child: CachedNetworkImage(
          imageUrl: photoUrl, width: 64, height: 64, fit: BoxFit.cover,
          errorWidget: (_, __, ___) => _initials(initials),
        ),
      );
    }
    return _initials(initials);
  }

  Widget _initials(String initials) => Container(
        width: 64, height: 64,
        decoration: const BoxDecoration(color: AppColors.primarySoft, shape: BoxShape.circle),
        alignment: Alignment.center,
        child: Text(initials,
            style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: 26)),
      );

  Widget _tile(BuildContext context, String icon, Color color, String title, VoidCallback onTap,
      {bool danger = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: Material(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          child: Ink(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.md),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
              boxShadow: AppShadows.card,
            ),
            child: Row(
              children: [
                Container(
                  width: 40, height: 40,
                  decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(12)),
                  alignment: Alignment.center,
                  child: AppIcon(icon, size: 21, color: color),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Text(title,
                      style: AppText.bodyStrong.copyWith(color: danger ? AppColors.danger : AppColors.ink)),
                ),
                if (!danger) const AppIcon('chevron-right', size: 20, color: AppColors.muted),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
