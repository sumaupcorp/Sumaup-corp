import 'package:dio/dio.dart';

import '../../../core/errors/app_exception.dart';
import '../domain/honorarios_models.dart';

class HonorariosRepository {
  HonorariosRepository(this._dio);
  final Dio _dio;

  // --- Recibos por Honorarios ---

  Future<HonorarioRequestModel> createHonorario({
    required String clienteNombre,
    String? clienteDocType,
    String? clienteDocNumber,
    required String descripcion,
    required double monto,
    bool conRetencion = false,
  }) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/honorarios', data: {
        'clienteNombre': clienteNombre,
        'clienteDocType': clienteDocType,
        'clienteDocNumber': clienteDocNumber,
        'descripcion': descripcion,
        'monto': monto,
        'conRetencion': conRetencion,
      });
      return HonorarioRequestModel.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<HonorarioRequestModel>> honorarios() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/honorarios');
      return (res.data ?? []).map((e) => HonorarioRequestModel.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  // --- Suspension de 4ta (anual) ---

  Future<SuspensionRequestModel> createSuspension(int anio) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/suspensiones', data: {'anio': anio});
      return SuspensionRequestModel.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<SuspensionRequestModel>> suspensiones() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/suspensiones');
      return (res.data ?? []).map((e) => SuspensionRequestModel.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
