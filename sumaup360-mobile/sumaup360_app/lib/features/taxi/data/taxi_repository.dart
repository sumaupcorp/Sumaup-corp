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

  Future<List<ReceiptRequestModel>> requests({String? origen}) async {
    try {
      final res = await _dio.get<List<dynamic>>(
        '/app/taxi/requests',
        queryParameters: {
          if (origen != null && origen.isNotEmpty) 'origen': origen,
        },
      );
      return (res.data ?? []).map((e) => ReceiptRequestModel.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Crea una solicitud de factura corporativa generada directamente por el taxista hacia la empresa.
  Future<void> createCorporateRequest({
    required double monto,
    required String docType,
    required String docNumber,
    required String customerName,
    required String whatsapp,
    String? email,
    String? observacion,
  }) async {
    try {
      await _dio.post<Map<String, dynamic>>(
        '/app/taxi/corporate-requests',
        data: {
          'monto': monto,
          'docType': docType,
          'docNumber': docNumber,
          'name': customerName,
          'customerName': customerName,
          'whatsapp': whatsapp,
          if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
          if (observacion != null && observacion.trim().isNotEmpty) 'observacion': observacion.trim(),
        },
      );
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Edita los datos de una solicitud de factura corporativa pendiente.
  Future<void> updateCorporateRequest({
    required String id,
    required double monto,
    required String docType,
    required String docNumber,
    required String customerName,
    required String whatsapp,
    String? email,
    String? observacion,
  }) async {
    try {
      await _dio.put<Map<String, dynamic>>(
        '/app/taxi/corporate-requests/$id',
        data: {
          'monto': monto,
          'docType': docType,
          'docNumber': docNumber,
          'name': customerName,
          'customerName': customerName,
          'whatsapp': whatsapp,
          if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
          if (observacion != null && observacion.trim().isNotEmpty) 'observacion': observacion.trim(),
        },
      );
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
      if (e.response?.statusCode == 404 || e.response?.statusCode == 500) {
        return const <CorporateRide>[];
      }
      throw AppException.fromDio(e);
    } catch (_) {
      return const <CorporateRide>[];
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

  /// Crea una solicitud de comprobante (factura/boleta).
  Future<void> createRequest({
    String? token,
    required String tipo,
    required double monto,
    required String docType,
    required String docNumber,
    required String name,
    required String whatsapp,
    String? email,
    String? observacion,
  }) async {
    try {
      final t = (token != null && token.isNotEmpty) ? token : await qrToken();
      await _dio.post<Map<String, dynamic>>(
        '/public/qr/taxi/$t/request',
        data: {
          'tipo': tipo,
          'monto': monto,
          'docType': docType,
          'docNumber': docNumber,
          'name': name,
          'whatsapp': whatsapp,
          if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
          if (observacion != null && observacion.trim().isNotEmpty) 'observacion': observacion.trim(),
        },
      );
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Consulta el nombre o razón social por documento del cliente.
  Future<String?> lookupCustomer({String? token, required String doc}) async {
    try {
      final t = (token != null && token.isNotEmpty) ? token : await qrToken();
      final res = await _dio.get<Map<String, dynamic>>(
        '/public/qr/taxi/$t/customers/lookup',
        queryParameters: {'doc': doc.trim()},
      );
      if (res.data?['found'] == true && res.data?['name'] != null) {
        return res.data!['name'] as String;
      }
      return null;
    } catch (_) {
      return null;
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
