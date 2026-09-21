import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../../core/config/env.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../shared/widgets/app_icon.dart';
import '../../application/taxi_providers.dart';

/// Tarjeta del QR unico del taxista para sus carreras directas.
///
/// El QR es unico y estatico (mismo token del backend). El pasajero lo escanea,
/// pide su comprobante y la carrera queda registrada automaticamente.
class TaxiQrCard extends ConsumerWidget {
  const TaxiQrCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final token = ref.watch(qrTokenProvider);
    return token.when(
      loading: () => const _QrSkeleton(),
      error: (_, __) => const _QrSkeleton(),
      data: (t) => _QrCard(token: t),
    );
  }
}

class _QrCard extends StatelessWidget {
  const _QrCard({required this.token});
  final String token;

  @override
  Widget build(BuildContext context) {
    final url = Env.qrUrl(token);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(13)),
            alignment: Alignment.center,
            child: const AppIcon('folder-share', size: 24, color: AppColors.primary),
          ),
          const SizedBox(height: AppSpacing.md),
          const Text('Tu QR para carreras directas', style: AppText.title, textAlign: TextAlign.center),
          const SizedBox(height: 4),
          const Text(
            'El pasajero lo escanea, pide su comprobante y la carrera se registra automaticamente.',
            textAlign: TextAlign.center,
            style: AppText.small,
          ),
          const SizedBox(height: AppSpacing.lg),
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(AppSpacing.radius),
              border: Border.all(color: AppColors.border),
            ),
            child: token.isEmpty
                ? const SizedBox(
                    width: 200,
                    height: 200,
                    child: Center(child: CircularProgressIndicator(color: AppColors.primary)),
                  )
                : QrImageView(data: url, version: QrVersions.auto, size: 200),
          ),
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: url));
                    ScaffoldMessenger.of(context)
                        .showSnackBar(const SnackBar(content: Text('Enlace copiado.')));
                  },
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primary,
                    side: BorderSide(color: AppColors.primary.withValues(alpha: 0.5)),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                  ),
                  icon: const AppIcon('folder-share', size: 18, color: AppColors.primary),
                  label: const Text('Copiar enlace'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QrSkeleton extends StatelessWidget {
  const _QrSkeleton();
  @override
  Widget build(BuildContext context) => Container(
        height: 300,
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          boxShadow: AppShadows.card,
        ),
        alignment: Alignment.center,
        child: const CircularProgressIndicator(color: AppColors.primary),
      );
}
