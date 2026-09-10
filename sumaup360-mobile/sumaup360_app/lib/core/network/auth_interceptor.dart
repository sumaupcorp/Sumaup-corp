import 'package:dio/dio.dart';
import 'package:firebase_auth/firebase_auth.dart';

/// Inyecta el Firebase idToken como `Authorization: Bearer <idToken>` en cada request.
/// Ante un 401, fuerza el refresh del idToken y reintenta una vez.
class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._dio);

  final Dio _dio;

  Future<String?> _token({bool forceRefresh = false}) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) return null;
    return user.getIdToken(forceRefresh);
  }

  @override
  Future<void> onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await _token();
    if (token != null) options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final isAuth = err.response?.statusCode == 401;
    final alreadyRetried = err.requestOptions.extra['retried'] == true;
    if (isAuth && !alreadyRetried) {
      final fresh = await _token(forceRefresh: true);
      if (fresh != null) {
        final opts = err.requestOptions
          ..headers['Authorization'] = 'Bearer $fresh'
          ..extra['retried'] = true;
        try {
          final clone = await _dio.fetch(opts);
          return handler.resolve(clone);
        } catch (_) {
          // cae al manejo normal
        }
      }
    }
    handler.next(err);
  }
}
