import 'package:dio/dio.dart';

/// Excepcion de dominio con mensaje listo para mostrar al usuario (en espanol).
class AppException implements Exception {
  AppException(this.message, {this.statusCode});
  final String message;
  final int? statusCode;

  @override
  String toString() => message;

  /// Traduce errores de Dio a un mensaje claro.
  factory AppException.fromDio(DioException e) {
    final code = e.response?.statusCode;
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.connectionError) {
      return AppException('No pudimos conectar. Revisa tu internet.', statusCode: code);
    }
    final data = e.response?.data;
    String? serverMsg;
    if (data is Map && data['message'] is String) serverMsg = data['message'] as String;
    switch (code) {
      case 401:
        return AppException(serverMsg ?? 'Tu sesion expiro. Inicia sesion de nuevo.', statusCode: 401);
      case 403:
        return AppException(serverMsg ?? 'No tienes permiso para esta accion.', statusCode: 403);
      case 404:
        return AppException(serverMsg ?? 'No encontramos lo que buscabas.', statusCode: 404);
      default:
        return AppException(serverMsg ?? 'Ocurrio un error. Intenta de nuevo.', statusCode: code);
    }
  }
}
