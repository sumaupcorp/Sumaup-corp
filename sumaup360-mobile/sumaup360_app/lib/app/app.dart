import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/theme/app_theme.dart';
import '../features/auth/application/auth_providers.dart';
import '../features/notifications/application/push_service.dart';
import 'router.dart';

/// Raiz de la app: MaterialApp.router con tema de marca y GoRouter.
/// Arranca el servicio de push cuando hay sesion iniciada.
class SumaupApp extends ConsumerWidget {
  const SumaupApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Con sesion: registra el token FCM y escucha notificaciones. La baja del
    // token al cerrar sesion se hace en los puntos de signOut (requiere idToken).
    ref.listen(authStateProvider, (_, next) {
      if (next.valueOrNull != null) {
        ref.read(pushServiceProvider).start();
      }
    });
    return MaterialApp.router(
      title: 'SUMAUP360',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      routerConfig: appRouter,
      scaffoldMessengerKey: rootScaffoldMessengerKey,
    );
  }
}
