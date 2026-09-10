/// Configuracion de entorno. La base del API se puede sobreescribir en tiempo de
/// compilacion con: flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
///
/// Nota: en el emulador Android, localhost del backend es 10.0.2.2.
class Env {
  Env._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static const String apiPrefix = '/api/v1';

  /// Base de la web publica del QR (donde el cliente del taxista pide su comprobante).
  static const String qrWebBase = String.fromEnvironment(
    'QR_WEB_BASE',
    defaultValue: 'https://sumaup360.com',
  );

  static String qrUrl(String token) => '$qrWebBase/qr/taxi/$token';

  static const Duration connectTimeout = Duration(seconds: 20);
  static const Duration receiveTimeout = Duration(seconds: 30);
}
