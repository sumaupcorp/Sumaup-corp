import 'package:flutter/material.dart';

/// Paleta de marca SUMAUP360 (Linea Personas). Azul/celeste/blanco, fondo claro.
class AppColors {
  AppColors._();

  static const Color primary = Color(0xFF0B5BFF); // azul principal
  static const Color primaryDark = Color(0xFF0A3DA8);
  static const Color primarySoft = Color(0xFFE8F0FF); // fondo azul suave
  static const Color accent = Color(0xFF3478F6);

  static const Color ink = Color(0xFF0F172A); // texto principal
  static const Color body = Color(0xFF475569); // texto secundario
  static const Color muted = Color(0xFF94A3B8); // texto terciario / hints

  static const Color bg = Color(0xFFFFFFFF); // fondo app
  static const Color surface = Color(0xFFFFFFFF); // cards
  static const Color surfaceAlt = Color(0xFFF6F8FC); // fondo suave de secciones
  static const Color border = Color(0xFFE2E8F0);

  static const Color success = Color(0xFF16A34A);
  static const Color warning = Color(0xFFD97706);
  static const Color danger = Color(0xFFDC2626);

  static const Color splashBg = Color(0xFF0A2A66); // azul oscuro del splash

  // Degradado del splash (0% -> 28% -> 62% -> 100%)
  static const List<Color> splashGradient = [
    Color(0xFF123C9C),
    Color(0xFF0D1B63),
    Color(0xFF0A1046),
    Color(0xFF050718),
  ];
  static const List<double> splashGradientStops = [0.0, 0.28, 0.62, 1.0];

  // Inicio / auth (colores medidos de los mockups de Figma)
  static const Color loginWave = Color(0xFF037EEE); // azul principal de la onda
  static const Color loginWaveLight = Color(0xFF3598F1); // circulo celeste
  static const Color authLink = Color(0xFF037EEE); // enlaces (Registrate, Terminos)
  static const Color authFieldBorder = Color(0xFFD7DEE4); // borde de inputs
  static const Color authHint = Color(0xFF9AA6B2); // placeholder / iconos de input
  static const Color authSubtitle = Color(0xFF64748B); // subtitulo bajo el titulo

  // Flujo de registro (mockups registro-*.png)
  // Degradado vertical: mas claro arriba (037EEE) a mas oscuro abajo (024888).
  static const List<Color> registerHeaderGradient = [
    Color(0xFF037EEE),
    Color(0xFF024888),
  ];
  static const Color optionCardBg = Color(0xFFF1F2F4); // tarjeta de opcion sin seleccionar
  static const Color quizSegment = Color(0xFFE3E6EA); // segmento inactivo del progreso

  // Onboarding
  static const Color onboardingTitle = Color(0xFF414D54); // titulos de slides
  static const Color onboardingButton = Color(0xFF272727); // boton oscuro
  static const Color onboardingButtonText = Color(0xFFF2F2F2);
  static const Color dotInactive = Color(0xFFD0D5DD);

  // Categorias de graficos
  static const List<Color> chart = [
    Color(0xFF0B5BFF),
    Color(0xFF3478F6),
    Color(0xFF60A5FA),
    Color(0xFF93C5FD),
    Color(0xFFF59E0B),
    Color(0xFF10B981),
  ];
}
