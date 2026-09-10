import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../../shared/widgets/money_total_card.dart';
import '../../../shared/widgets/transaction_tile.dart';
import '../application/income_providers.dart';

class IncomeScreen extends ConsumerWidget {
  const IncomeScreen({super.key, this.embedded = false});

  /// embedded = se muestra dentro del shell de Home (sin AppBar propio).
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(incomeListProvider);
    final total = ref.watch(incomeMonthTotalProvider);

    final body = async.when(
      loading: () => const LoadingView(),
      error: (e, _) => ErrorView(message: '$e', onRetry: () => ref.invalidate(incomeListProvider)),
      data: (items) => RefreshIndicator(
        onRefresh: () async => ref.invalidate(incomeListProvider),
        child: ListView(
          padding: AppSpacing.screen,
          children: [
            MoneyTotalCard(label: 'Ingresos de este mes', value: Fmt.money(total), color: AppColors.success),
            const SizedBox(height: AppSpacing.lg),
            if (items.isEmpty)
              const EmptyHint(text: 'Aun no registras ingresos.')
            else
              ...items.map((i) => TransactionTile(
                    title: i.category ?? 'Ingreso',
                    subtitle: '${Fmt.date(i.txDate)}${i.description != null ? ' · ${i.description}' : ''}',
                    amount: '+ ${Fmt.money(i.amount)}',
                    color: AppColors.success,
                  )),
          ],
        ),
      ),
    );

    final fab = FloatingActionButton.extended(
      onPressed: () => context.push(Routes.incomeForm),
      backgroundColor: AppColors.primary,
      icon: const AppIcon('plus', color: Colors.white),
      label: const Text('Ingreso', style: TextStyle(color: Colors.white)),
    );

    if (embedded) return Scaffold(body: body, floatingActionButton: fab);
    return Scaffold(appBar: AppBar(title: const Text('Ingresos')), body: body, floatingActionButton: fab);
  }
}
