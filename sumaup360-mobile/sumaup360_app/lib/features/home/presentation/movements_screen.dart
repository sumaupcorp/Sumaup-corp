import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/money_total_card.dart';
import '../../../shared/widgets/transaction_tile.dart';
import '../../expenses/application/expense_providers.dart';
import '../../income/application/income_providers.dart';

class MovementsScreen extends ConsumerWidget {
  const MovementsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final incomes = ref.watch(incomeListProvider).valueOrNull ?? [];
    final expenses = ref.watch(expenseListProvider).valueOrNull ?? [];
    
    final items = <(DateTime, String, String, Color, String)>[
      ...incomes.map((i) => (i.txDate, i.category ?? 'Ingreso', '+ ${Fmt.money(i.amount)}', AppColors.success, Fmt.date(i.txDate))),
      ...expenses.map((e) => (e.txDate, e.category ?? 'Gasto', '- ${Fmt.money(e.amount)}', AppColors.danger, Fmt.date(e.txDate))),
    ]..sort((a, b) => b.$1.compareTo(a.$1));

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Historial de Movimientos')),
      body: items.isEmpty
          ? const Center(child: EmptyHint(text: 'Aun no registras movimientos.'))
          : ListView.builder(
              padding: AppSpacing.screen,
              itemCount: items.length,
              itemBuilder: (context, index) {
                final m = items[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.md),
                  child: TransactionTile(
                    title: m.$2,
                    subtitle: m.$5,
                    amount: m.$3,
                    color: m.$4,
                  ),
                );
              },
            ),
    );
  }
}
