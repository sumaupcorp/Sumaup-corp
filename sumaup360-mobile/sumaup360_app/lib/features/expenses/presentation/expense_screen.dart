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
import '../application/expense_providers.dart';

class ExpenseScreen extends ConsumerWidget {
  const ExpenseScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(expenseListProvider);
    final total = ref.watch(expenseMonthTotalProvider);

    final body = async.when(
      loading: () => const LoadingView(),
      error: (e, _) => ErrorView(message: '$e', onRetry: () => ref.invalidate(expenseListProvider)),
      data: (items) => RefreshIndicator(
        onRefresh: () async => ref.invalidate(expenseListProvider),
        child: ListView(
          padding: AppSpacing.screen,
          children: [
            MoneyTotalCard(label: 'Gastos de este mes', value: Fmt.money(total), color: AppColors.danger),
            const SizedBox(height: AppSpacing.lg),
            if (items.isEmpty)
              const EmptyHint(text: 'Aun no registras gastos.')
            else
              ...items.map((e) => TransactionTile(
                    title: e.category ?? 'Gasto',
                    subtitle: '${Fmt.date(e.txDate)}${e.description != null ? ' · ${e.description}' : ''}',
                    amount: '- ${Fmt.money(e.amount)}',
                    color: AppColors.danger,
                  )),
          ],
        ),
      ),
    );

    final fab = FloatingActionButton.extended(
      onPressed: () => context.push(Routes.expenseForm),
      backgroundColor: AppColors.primary,
      icon: const AppIcon('plus', color: Colors.white),
      label: const Text('Gasto', style: TextStyle(color: Colors.white)),
    );

    if (embedded) return Scaffold(body: body, floatingActionButton: fab);
    return Scaffold(appBar: AppBar(title: const Text('Gastos')), body: body, floatingActionButton: fab);
  }
}
