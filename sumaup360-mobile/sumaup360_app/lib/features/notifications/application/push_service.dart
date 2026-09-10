import 'dart:async';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../data/device_token_repository.dart';

/// Key global del ScaffoldMessenger raiz: permite mostrar la notificacion en
/// primer plano (banner) desde fuera del arbol de widgets.
final rootScaffoldMessengerKey = GlobalKey<ScaffoldMessengerState>();

/// Servicio de notificaciones push:
/// - registra el token FCM en el backend cuando hay sesion (y en cada refresh),
/// - muestra un banner cuando llega un push con la app abierta,
/// - navega a la ruta indicada en data['route'] al tocar la notificacion.
///
/// En background/terminada, los mensajes con bloque "notification" los muestra
/// el sistema automaticamente; aqui solo se maneja el tap de apertura.
class PushService {
  PushService(this._ref);
  final Ref _ref;

  bool _started = false;
  String? _token;
  StreamSubscription<String>? _tokenSub;
  StreamSubscription<RemoteMessage>? _messageSub;
  StreamSubscription<RemoteMessage>? _openedSub;

  String get _platform {
    if (kIsWeb) return 'web';
    return defaultTargetPlatform == TargetPlatform.iOS ? 'ios' : 'android';
  }

  /// Arranca al iniciar sesion. Fail-safe: un push nunca debe romper la app.
  Future<void> start() async {
    if (_started) return;
    _started = true;
    try {
      final messaging = FirebaseMessaging.instance;

      final token = await messaging.getToken();
      if (token != null) await _register(token);

      _tokenSub = messaging.onTokenRefresh.listen(_register);
      _messageSub = FirebaseMessaging.onMessage.listen(_showForeground);
      _openedSub = FirebaseMessaging.onMessageOpenedApp.listen(_openFromMessage);

      // App abierta desde terminada tocando una notificacion.
      final initial = await messaging.getInitialMessage();
      if (initial != null) _openFromMessage(initial);
    } catch (e) {
      debugPrint('Push no disponible: $e');
    }
  }

  /// Da de baja el token en el backend. Llamar ANTES de cerrar sesion
  /// (la baja necesita el idToken vigente).
  Future<void> stop() async {
    final token = _token;
    _token = null;
    _started = false;
    await _tokenSub?.cancel();
    await _messageSub?.cancel();
    await _openedSub?.cancel();
    if (token != null) {
      try {
        await _ref.read(deviceTokenRepositoryProvider).unregister(token: token);
      } catch (_) {
        // Sin conexion o sesion vencida: el backend lo limpiara al fallar el envio.
      }
    }
  }

  Future<void> _register(String token) async {
    try {
      await _ref.read(deviceTokenRepositoryProvider).register(token: token, platform: _platform);
      _token = token;
    } catch (e) {
      debugPrint('No se pudo registrar el token push: $e');
    }
  }

  /// Banner simple cuando el push llega con la app en primer plano.
  void _showForeground(RemoteMessage message) {
    final title = message.notification?.title ?? '';
    final body = message.notification?.body ?? '';
    if (title.isEmpty && body.isEmpty) return;
    final route = message.data['route'] as String?;
    rootScaffoldMessengerKey.currentState?.showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        backgroundColor: AppColors.ink,
        duration: const Duration(seconds: 5),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (title.isNotEmpty)
              Text(title,
                  style: const TextStyle(
                      fontFamily: 'Poppins', fontWeight: FontWeight.w600, color: Colors.white)),
            if (body.isNotEmpty)
              Text(body,
                  style: const TextStyle(fontFamily: 'Poppins', fontSize: 13, color: Colors.white)),
          ],
        ),
        action: (route == null || route.isEmpty)
            ? null
            : SnackBarAction(label: 'Ver', textColor: AppColors.loginWaveLight, onPressed: () => _go(route)),
      ),
    );
  }

  void _openFromMessage(RemoteMessage message) {
    final route = message.data['route'] as String?;
    if (route != null && route.isNotEmpty) _go(route);
  }

  void _go(String route) {
    try {
      appRouter.push(route);
    } catch (_) {
      appRouter.go(route);
    }
  }
}

final pushServiceProvider = Provider<PushService>((ref) {
  final service = PushService(ref);
  ref.onDispose(service.stop);
  return service;
});
