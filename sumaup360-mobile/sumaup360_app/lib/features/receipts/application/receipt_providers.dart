import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/receipt_repository.dart';
import '../domain/receipt.dart';

final receiptRepositoryProvider = Provider<ReceiptRepository>((ref) => ReceiptRepository(ref.watch(dioProvider)));

final receiptListProvider = FutureProvider<List<Receipt>>((ref) {
  return ref.watch(receiptRepositoryProvider).list();
});
