import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/expense_repository.dart';
import '../domain/expense.dart';

final expenseRepositoryProvider = Provider<ExpenseRepository>((ref) => ExpenseRepository(ref.watch(dioProvider)));

final expenseListProvider = FutureProvider<List<Expense>>((ref) {
  return ref.watch(expenseRepositoryProvider).list();
});

final expenseMonthTotalProvider = Provider<double>((ref) {
  final list = ref.watch(expenseListProvider).valueOrNull ?? [];
  final now = DateTime.now();
  return list
      .where((e) => e.txDate.year == now.year && e.txDate.month == now.month)
      .fold<double>(0, (s, e) => s + e.amount);
});

/// Totales por categoria del mes actual (para grafico de gastos).
final expenseByCategoryProvider = Provider<Map<String, double>>((ref) {
  final list = ref.watch(expenseListProvider).valueOrNull ?? [];
  final now = DateTime.now();
  final map = <String, double>{};
  for (final e in list.where((e) => e.txDate.year == now.year && e.txDate.month == now.month)) {
    final key = e.category ?? 'Otros';
    map[key] = (map[key] ?? 0) + e.amount;
  }
  return map;
});
