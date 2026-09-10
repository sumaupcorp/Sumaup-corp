# Despliegue — App móvil (sumaup360-mobile)

Notas de build y publicación de la app Flutter. Detalle final en Fase 9.

## Build
```
cd sumaup360-mobile/sumaup360_app
flutter pub get
dart run build_runner build --delete-conflicting-outputs   # Freezed / json_serializable
flutter build apk        # Android
flutter build appbundle  # Play Store
flutter build ios        # iOS (requiere macOS + Xcode)
```

## Configuración Firebase
- Mismo proyecto Firebase del ecosistema.
- `flutterfire configure` genera `firebase_options.dart` (NO commitear).
- `google-services.json` (Android) y `GoogleService-Info.plist` (iOS) fuera del control de
  versiones.

## Variables / secretos
- URL base del backend (API).
- Tokens en `flutter_secure_storage`, nunca en texto plano.

## Checklist
- `flutter analyze` y `flutter test` verdes.
- Estados de carga/error/vacío en pantallas que consumen datos.
- idToken Firebase enviado en cada request (interceptor Dio).
- Identidad de marca Suma; español; sin emojis.
```
