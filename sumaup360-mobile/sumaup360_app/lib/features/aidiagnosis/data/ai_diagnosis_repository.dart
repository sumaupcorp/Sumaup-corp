import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/providers.dart';

/// Reporte de diagnostico generado con IA.
class DiagnosisReport {
  const DiagnosisReport({required this.id, required this.resultado, this.modelo, this.createdAt});
  final String id;
  final String resultado;
  final String? modelo;
  final String? createdAt;

  factory DiagnosisReport.fromJson(Map<String, dynamic> j) => DiagnosisReport(
        id: j['id'] as String,
        resultado: (j['resultado'] as String?) ?? '',
        modelo: j['modelo'] as String?,
        createdAt: j['createdAt'] as String?,
      );
}

/// Estado del diagnostico: si tiene su intento disponible y su ultimo reporte.
class DiagnosisStatus {
  const DiagnosisStatus({required this.available, this.latest});
  final bool available;
  final DiagnosisReport? latest;

  factory DiagnosisStatus.fromJson(Map<String, dynamic> j) => DiagnosisStatus(
        available: (j['available'] as bool?) ?? true,
        latest: j['latest'] is Map<String, dynamic>
            ? DiagnosisReport.fromJson(j['latest'] as Map<String, dynamic>)
            : null,
      );
}

class AiDiagnosisRepository {
  AiDiagnosisRepository(this._dio);
  final Dio _dio;

  Future<DiagnosisStatus> status() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/ai-diagnosis/status');
      return DiagnosisStatus.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<DiagnosisReport> generate({required String doc, required String docType}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/ai-diagnosis/generate',
          data: {'doc': doc, 'docType': docType});
      return DiagnosisReport.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<DiagnosisReport>> reports() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/ai-diagnosis/reports');
      return (res.data ?? []).map((e) => DiagnosisReport.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}

final aiDiagnosisRepositoryProvider =
    Provider<AiDiagnosisRepository>((ref) => AiDiagnosisRepository(ref.watch(dioProvider)));

final aiDiagnosisStatusProvider =
    FutureProvider<DiagnosisStatus>((ref) => ref.watch(aiDiagnosisRepositoryProvider).status());
