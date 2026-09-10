import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/providers.dart';
import '../domain/summary.dart';

final summaryProvider = FutureProvider<MonthlySummary>((ref) async {
  final dio = ref.watch(dioProvider);
  try {
    final res = await dio.get<Map<String, dynamic>>('/app/summary');
    return MonthlySummary.fromJson(res.data ?? {});
  } on DioException catch (e) {
    throw AppException.fromDio(e);
  }
});
