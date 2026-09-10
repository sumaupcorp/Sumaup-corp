import 'package:dio/dio.dart';
import '../../../core/errors/app_exception.dart';
import '../domain/expense.dart';

class ExpenseRepository {
  ExpenseRepository(this._dio);
  final Dio _dio;

  Future<List<Expense>> list() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/expenses');
      return (res.data ?? []).map((e) => Expense.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<void> create({
    required DateTime txDate,
    required double amount,
    String? category,
    String? description,
    String? paymentMethod,
    String? receiptId,
  }) async {
    try {
      await _dio.post<Map<String, dynamic>>('/app/expenses', data: {
        'txDate': txDate.toIso8601String().substring(0, 10),
        'amount': amount,
        'currency': 'PEN',
        'category': category,
        'paymentMethod': paymentMethod,
        'description': description,
        'receiptId': receiptId,
      });
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<void> delete(String id) async {
    try {
      await _dio.delete<void>('/app/expenses/$id');
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
