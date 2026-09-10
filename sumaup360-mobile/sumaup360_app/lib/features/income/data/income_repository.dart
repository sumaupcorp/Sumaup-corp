import 'package:dio/dio.dart';
import '../../../core/errors/app_exception.dart';
import '../domain/income.dart';

class IncomeRepository {
  IncomeRepository(this._dio);
  final Dio _dio;

  Future<List<Income>> list() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/income');
      return (res.data ?? []).map((e) => Income.fromJson(e as Map<String, dynamic>)).toList();
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
      await _dio.post<Map<String, dynamic>>('/app/income', data: {
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
      await _dio.delete<void>('/app/income/$id');
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
