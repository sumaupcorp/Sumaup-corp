/// Resumen mensual (mapea SummaryResponse del backend).
class MonthlySummary {
  MonthlySummary({
    required this.period,
    required this.totalIncome,
    required this.totalExpense,
    required this.utility,
    required this.currency,
  });

  final String period;
  final double totalIncome;
  final double totalExpense;
  final double utility;
  final String currency;

  factory MonthlySummary.fromJson(Map<String, dynamic> j) => MonthlySummary(
        period: (j['period'] as String?) ?? '',
        totalIncome: (j['totalIncome'] as num?)?.toDouble() ?? 0,
        totalExpense: (j['totalExpense'] as num?)?.toDouble() ?? 0,
        utility: (j['utility'] as num?)?.toDouble() ?? 0,
        currency: (j['currency'] as String?) ?? 'PEN',
      );
}
