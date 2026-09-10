import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Almacenamiento seguro (Keychain/Keystore). Guarda banderas locales y, si hiciera falta,
/// cache del idToken. La contrasena NUNCA se guarda.
class SecureStorageService {
  SecureStorageService([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _kOnboardingSeen = 'onboarding_seen';

  Future<void> write(String key, String value) => _storage.write(key: key, value: value);
  Future<String?> read(String key) => _storage.read(key: key);
  Future<void> delete(String key) => _storage.delete(key: key);
  Future<void> clear() => _storage.deleteAll();

  Future<bool> isOnboardingSeen() async => (await _storage.read(key: _kOnboardingSeen)) == 'true';
  Future<void> setOnboardingSeen() => _storage.write(key: _kOnboardingSeen, value: 'true');
}
