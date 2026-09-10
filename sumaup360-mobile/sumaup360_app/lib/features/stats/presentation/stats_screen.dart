import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../expenses/application/expense_providers.dart';
import '../../income/application/income_providers.dart';
import '../../home/application/summary_providers.dart';

class StatsScreen extends ConsumerWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(summaryProvider);
    final income = ref.watch(incomeMonthTotalProvider);
    final expense = ref.watch(expenseMonthTotalProvider);
    final byCat = ref.watch(expenseByCategoryProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Estadisticas')),
      body: summary.when(
        loading: () => const LoadingView(),
        error: (e, _) => Center(child: Text('$e')),
        data: (s) => ListView(
          padding: AppSpacing.screen,
          children: [
            const Text('Resumen del mes', style: AppText.title),
            const SizedBox(height: AppSpacing.md),
            Row(
              children: [
                _Stat(label: 'Ingresos', value: Fmt.money(income), color: AppColors.success),
                _Stat(label: 'Gastos', value: Fmt.money(expense), color: AppColors.danger),
                _Stat(label: 'Balance', value: Fmt.money(income - expense), color: AppColors.primary),
              ],
            ),
            const SizedBox(height: AppSpacing.xl),
            const Text('Ingresos vs gastos', style: AppText.title),
            const SizedBox(height: AppSpacing.md),
            SizedBox(height: 200, child: _IncomeExpenseBar(income: income, expense: expense)),
            const SizedBox(height: AppSpacing.xl),
            const Text('Gastos por categoria', style: AppText.title),
            const SizedBox(height: AppSpacing.md),
            if (byCat.isEmpty)
              const Text('Aun no hay gastos este mes.', style: AppText.small)
            else
              _CategoryPie(data: byCat),
          ],
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value, required this.color});
  final String label;
  final String value;
  final Color color;
  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 3),
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg, horizontal: AppSpacing.sm),
        decoration: BoxDecoration(
          color: AppColors.surfaceAlt,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          children: [
            Text(value, style: AppText.title.copyWith(color: color), textAlign: TextAlign.center),
            const SizedBox(height: 2),
            Text(label, style: AppText.small),
          ],
        ),
      ),
    );
  }
}

class _IncomeExpenseBar extends StatelessWidget {
  const _IncomeExpenseBar({required this.income, required this.expense});
  final double income;
  final double expense;
  @override
  Widget build(BuildContext context) {
    final maxY = (income > expense ? income : expense) * 1.2;
    return BarChart(
      BarChartData(
        maxY: maxY <= 0 ? 100 : maxY,
        borderData: FlBorderData(show: false),
        gridData: const FlGridData(show: false),
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (v, _) => Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(v == 0 ? 'Ingresos' : 'Gastos', style: AppText.small),
              ),
            ),
          ),
        ),
        barGroups: [
          BarChartGroupData(x: 0, barRods: [BarChartRodData(toY: income, color: AppColors.success, width: 42, borderRadius: BorderRadius.circular(8))]),
          BarChartGroupData(x: 1, barRods: [BarChartRodData(toY: expense, color: AppColors.danger, width: 42, borderRadius: BorderRadius.circular(8))]),
        ],
      ),
    );
  }
}

class _CategoryPie extends StatelessWidget {
  const _CategoryPie({required this.data});
  final Map<String, double> data;
  @override
  Widget build(BuildContext context) {
    final entries = data.entries.toList();
    final total = entries.fold<double>(0, (s, e) => s + e.value);
    return Column(
      children: [
        SizedBox(
          height: 180,
          child: PieChart(
            PieChartData(
              centerSpaceRadius: 44,
              sectionsSpace: 2,
              sections: [
                for (var i = 0; i < entries.length; i++)
                  PieChartSectionData(
                    value: entries[i].value,
                    color: AppColors.chart[i % AppColors.chart.length],
                    title: '${(entries[i].value / total * 100).round()}%',
                    radius: 50,
                    titleStyle: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        Wrap(
          spacing: AppSpacing.md, runSpacing: AppSpacing.sm,
          children: [
            for (var i = 0; i < entries.length; i++)
              Row(mainAxisSize: MainAxisSize.min, children: [
                Container(width: 12, height: 12, decoration: BoxDecoration(
                    color: AppColors.chart[i % AppColors.chart.length], borderRadius: BorderRadius.circular(3))),
                const SizedBox(width: 6),
                Text('${entries[i].key} · ${Fmt.money(entries[i].value)}', style: AppText.small),
              ]),
          ],
        ),
      ],
    );
  }
}
