import 'package:dio/dio.dart';
import '../config/env.dart';
import 'auth_interceptor.dart';

/// Cliente HTTP central. baseUrl ya incluye el prefijo /api/v1, por lo que los
/// endpoints se escriben relativos (ej. '/app/profile/me').
class ApiClient {
  ApiClient() {
    _dio = Dio(
      BaseOptions(
        baseUrl: '${Env.apiBaseUrl}${Env.apiPrefix}',
        connectTimeout: Env.connectTimeout,
        receiveTimeout: Env.receiveTimeout,
        contentType: Headers.jsonContentType,
      ),
    );
    _dio.interceptors.add(AuthInterceptor(_dio));
  }

  late final Dio _dio;
  Dio get dio => _dio;
}
