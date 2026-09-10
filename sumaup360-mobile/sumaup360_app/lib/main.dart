 // Punto de entrada de sumaup360_app (Linea Personas de SUMAUP360).
//
// Inicializa Firebase y arranca la app con Riverpod + GoRouter + tema de marca.
// Si Firebase falla al iniciar (falta google-services.json en dev), la app igual
// arranca para poder navegar onboarding/UI; el login real requiere Firebase configurado.

import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'firebase_options.dart';

/// Handler de push en background/terminada. Los mensajes con bloque
/// "notification" los muestra el sistema solo; aqui no hay nada que hacer,
/// pero FCM exige registrar un handler de nivel superior.
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Carga los simbolos de fecha en espanol (es / es_PE) usados por Fmt.
  await initializeDateFormatting('es');
  await initializeDateFormatting('es_PE');
  Intl.defaultLocale = 'es_PE';
  try {
    await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  } catch (e) {
    // En desarrollo, sin config nativa, continuamos para poder ver la UI.
    debugPrint('Firebase no inicializado: $e');
  }
  runApp(const ProviderScope(child: SumaupApp()));
}
