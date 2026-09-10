import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/providers.dart';

/// Registro del token FCM del dispositivo en el backend (notification.device_token).
class DeviceTokenRepository {
  DeviceTokenRepository(this._dio);
  final Dio _dio;

  /// Upsert del token del dispositivo para el usuario autenticado.
  Future<void> register({required String token, required String platform}) async {
    try {
      await _dio.put<void>('/app/notifications/device', data: {
        'token': token,
        'platform': platform,
      });
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Da de baja el token (al cerrar sesion). Idempotente.
  Future<void> unregister({required String token}) async {
    try {
      await _dio.delete<void>('/app/notifications/device', data: {'token': token});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}

final deviceTokenRepositoryProvider =
    Provider<DeviceTokenRepository>((ref) => DeviceTokenRepository(ref.watch(dioProvider)));
