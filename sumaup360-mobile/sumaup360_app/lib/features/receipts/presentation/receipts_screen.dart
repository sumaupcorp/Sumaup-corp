import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../../shared/widgets/money_total_card.dart';
import '../application/receipt_providers.dart';
import '../domain/receipt.dart';

class ReceiptsScreen extends ConsumerWidget {
  const ReceiptsScreen({super.key, this.embedded = false});
  final bool embedded;

  Color _statusColor(String s) {
    switch (s) {
      case 'PROCESSED':
        return AppColors.success;
      case 'OBSERVED':
        return AppColors.warning;
      case 'IN_PROCESS':
        return AppColors.primary;
      default:
        return AppColors.muted;
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(receiptListProvider);

    final body = async.when(
      loading: () => const LoadingView(),
      error: (e, _) => ErrorView(message: '$e', onRetry: () => ref.invalidate(receiptListProvider)),
      data: (items) => RefreshIndicator(
        onRefresh: () async => ref.invalidate(receiptListProvider),
        child: ListView(
          padding: AppSpacing.screen,
          children: [
            if (items.isEmpty)
              const EmptyHint(text: 'Aun no subes comprobantes. Toca "Subir" para empezar.')
            else
              ...items.map((r) => Container(
                    margin: const EdgeInsets.only(bottom: AppSpacing.sm),
                    padding: const EdgeInsets.all(AppSpacing.md),
                    decoration: BoxDecoration(
                      color: AppColors.surface,
                      borderRadius: BorderRadius.circular(AppSpacing.radius),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Row(
                      children: [
                        Container(
                          height: 40, width: 40,
                          decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(10)),
                          alignment: Alignment.center,
                          child: const AppIcon('receipt-tax', color: AppColors.primary, size: 20),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(r.docNumber ?? 'Comprobante', style: AppText.bodyStrong),
                              const SizedBox(height: 2),
                              Text('${Fmt.date(r.issueDate)} · ${r.amount != null ? Fmt.money(r.amount) : 'sin monto'}',
                                  style: AppText.small),
                            ],
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _statusColor(r.status).withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(99),
                          ),
                          child: Text(receiptStatusLabel(r.status),
                              style: TextStyle(fontSize: 12, color: _statusColor(r.status), fontWeight: FontWeight.w600)),
                        ),
                      ],
                    ),
                  )),
          ],
        ),
      ),
    );

    final fab = FloatingActionButton.extended(
      onPressed: () => context.push(Routes.receiptUpload),
      backgroundColor: AppColors.primary,
      icon: const AppIcon('camera-add', color: Colors.white),
      label: const Text('Subir', style: TextStyle(color: Colors.white)),
    );

    if (embedded) return Scaffold(body: body, floatingActionButton: fab);
    return Scaffold(appBar: AppBar(title: const Text('Comprobantes')), body: body, floatingActionButton: fab);
  }
}
