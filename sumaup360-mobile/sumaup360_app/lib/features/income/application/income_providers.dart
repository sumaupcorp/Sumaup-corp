import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/income_repository.dart';
import '../domain/income.dart';

final incomeRepositoryProvider = Provider<IncomeRepository>((ref) => IncomeRepository(ref.watch(dioProvider)));

final incomeListProvider = FutureProvider<List<Income>>((ref) {
  return ref.watch(incomeRepositoryProvider).list();
});

/// Total de ingresos del mes actual.
final incomeMonthTotalProvider = Provider<double>((ref) {
  final list = ref.watch(incomeListProvider).valueOrNull ?? [];
  final now = DateTime.now();
  return list
      .where((i) => i.txDate.year == now.year && i.txDate.month == now.month)
      .fold<double>(0, (s, i) => s + i.amount);
});
