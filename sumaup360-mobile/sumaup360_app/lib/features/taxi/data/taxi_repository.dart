import 'package:dio/dio.dart';
import '../../../core/errors/app_exception.dart';
import '../domain/attachment.dart';
import '../domain/corporate_ride.dart';
import '../domain/receipt_request.dart';

class TaxiRepository {
  TaxiRepository(this._dio);
  final Dio _dio;

  Future<String> qrToken() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/taxi/qr');
      return (res.data?['token'] as String?) ?? '';
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Regenera el token del QR (invalida el anterior). Devuelve el nuevo token.
  Future<String> regenerateQr() async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/taxi/qr/regenerate');
      return (res.data?['token'] as String?) ?? '';
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<ReceiptRequestModel>> requests() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/taxi/requests');
      return (res.data ?? []).map((e) => ReceiptRequestModel.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<AttachmentModel>> requestAttachments(String id) async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/taxi/requests/$id/attachments');
      return (res.data ?? []).map((e) => AttachmentModel.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  // --- Corporativo: carreras pagadas con tarjeta que van directo a la empresa ---

  /// Carreras corporativas del taxista (pendientes de facturar + historial).
  Future<List<CorporateRide>> corporateRides() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/taxi/corporate/rides');
      return (res.data ?? []).map((e) => CorporateRide.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Genera la factura del taxista hacia la empresa por las carreras seleccionadas.
  Future<void> createCorporateInvoice(List<String> rideIds) async {
    try {
      await _dio.post<Map<String, dynamic>>('/app/taxi/corporate/invoices', data: {'rideIds': rideIds});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<void> confirm(String id) => _action('/app/taxi/requests/$id/confirm');

  Future<void> reject(String id, String motivo) => _action('/app/taxi/requests/$id/reject', {'motivo': motivo});

  Future<void> editMonto(String id, double monto, String? motivo) =>
      _action('/app/taxi/requests/$id/edit', {'monto': monto, 'motivo': motivo});

  Future<void> _action(String path, [Map<String, dynamic>? body]) async {
    try {
      await _dio.post<Map<String, dynamic>>(path, data: body ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
