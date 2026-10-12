import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/config/env.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../shared/widgets/app_icon.dart';
import '../application/taxi_providers.dart';
import 'solicitudes_list_screen.dart';
import 'widgets/qr_request_card.dart';

/// Modulo Comprobantes del taxista (Premium): su QR + solicitudes de clientes.
class ComprobantesScreen extends ConsumerWidget {
  const ComprobantesScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final token = ref.watch(qrTokenProvider);
    final requests = ref.watch(qrRequestsProvider);

    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(qrTokenProvider);
        ref.invalidate(qrRequestsProvider);
        ref.invalidate(taxiRequestsProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!embedded) ...[const Text('Comprobantes', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],
          token.when(
            loading: () => const _QrSkeleton(),
            error: (_, __) => const _QrSkeleton(),
            data: (t) => _QrCard(token: t),
          ),
          const SizedBox(height: AppSpacing.xl),
          requests.when(
            loading: () => const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Solicitudes de clientes', style: AppText.title),
                SizedBox(height: AppSpacing.md),
                Center(child: Padding(padding: EdgeInsets.all(AppSpacing.xl), child: CircularProgressIndicator(color: AppColors.primary))),
              ],
            ),
            error: (e, __) => const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Solicitudes de clientes', style: AppText.title),
                SizedBox(height: AppSpacing.md),
                Text('No pudimos cargar tus solicitudes.', style: AppText.small),
              ],
            ),
            data: (list) {
              final qrList = list.where((r) => r.isQr).toList();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Solicitudes de clientes', style: AppText.title),
                      if (qrList.length > 5)
                        TextButton(
                          onPressed: () => Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) => const SolicitudesListScreen(
                                origen: 'QR',
                                title: 'Solicitudes de clientes',
                              ),
                            ),
                          ),
                          child: Text('Ver todas (${qrList.length})'),
                        ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  if (qrList.isEmpty)
                    _empty()
                  else ...[
                    ...qrList.take(5).map((r) => QrRequestCard(request: r)),
                    if (qrList.length > 5) ...[
                      const SizedBox(height: AppSpacing.xs),
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton(
                          onPressed: () => Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (_) => const SolicitudesListScreen(
                                origen: 'QR',
                                title: 'Solicitudes de clientes',
                              ),
                            ),
                          ),
                          style: OutlinedButton.styleFrom(
                            minimumSize: const Size.fromHeight(44),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                          ),
                          child: Text('Ver todas las solicitudes (${qrList.length})'),
                        ),
                      ),
                    ],
                  ],
                ],
              );
            },
          ),
        ],
      ),
    );

    if (embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Comprobantes')),
      body: SafeArea(child: body),
    );
  }

  Widget _empty() => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpacing.xxl),
        decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
        child: const Text('Aun no tienes solicitudes. Comparte tu QR con tus clientes.',
            textAlign: TextAlign.center, style: AppText.small),
      );
}

class _QrCard extends ConsumerWidget {
  const _QrCard({required this.token});
  final String token;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final url = Env.qrUrl(token);
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        children: [
          const Text('Tu QR para clientes', style: AppText.title),
          const SizedBox(height: 4),
          const Text('Tu cliente lo escanea y solicita su boleta o factura.',
              textAlign: TextAlign.center, style: AppText.small),
          const SizedBox(height: AppSpacing.lg),
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(AppSpacing.radius)),
            child: token.isEmpty
                ? const SizedBox(width: 180, height: 180, child: Center(child: CircularProgressIndicator(color: AppColors.primary)))
                : QrImageView(data: url, version: QrVersions.auto, size: 180),
          ),
          const SizedBox(height: AppSpacing.md),
          Center(
            child: OutlinedButton.icon(
              onPressed: () {
                Clipboard.setData(ClipboardData(text: url));
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enlace copiado.')));
              },
              icon: const AppIcon('copy', size: 18, color: AppColors.primary),
              label: const Text('Copiar enlace'),
            ),
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
        height: 260,
        decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
        alignment: Alignment.center,
        child: const CircularProgressIndicator(color: AppColors.primary),
      );
}

