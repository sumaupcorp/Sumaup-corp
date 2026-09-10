import 'package:flutter/widgets.dart';

/// Sombras suaves de marca (look "flotante" tipo tarjeta).
class AppShadows {
  AppShadows._();

  /// Sombra sutil para cards sobre fondo claro.
  static const List<BoxShadow> card = [
    BoxShadow(color: Color(0x0F1E293B), blurRadius: 18, offset: Offset(0, 8)),
  ];

  /// Sombra mas marcada para elementos flotantes (navbar, hero).
  static const List<BoxShadow> floating = [
    BoxShadow(color: Color(0x1A1E293B), blurRadius: 24, offset: Offset(0, 12)),
  ];

  /// Sombra tintada en azul para el hero principal.
  static const List<BoxShadow> primary = [
    BoxShadow(color: Color(0x330B5BFF), blurRadius: 24, offset: Offset(0, 12)),
  ];
}

/// Espaciado y radios consistentes (mobile-first).
class AppSpacing {
  AppSpacing._();

  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 24;
  static const double xxl = 32;
  static const double xxxl = 48;

  // Radios
  static const double radiusSm = 10;
  static const double radius = 14;
  static const double radiusLg = 20;
  static const double radiusPill = 999;

  // Padding de pantalla
  static const EdgeInsets screen = EdgeInsets.symmetric(horizontal: lg, vertical: lg);
  static const EdgeInsets screenH = EdgeInsets.symmetric(horizontal: lg);
}

/// Atajos de SizedBox para separar widgets.
class Gap {
  Gap._();
  static const Widget xs = SizedBox(height: AppSpacing.xs, width: AppSpacing.xs);
  static const Widget sm = SizedBox(height: AppSpacing.sm, width: AppSpacing.sm);
  static const Widget md = SizedBox(height: AppSpacing.md, width: AppSpacing.md);
  static const Widget lg = SizedBox(height: AppSpacing.lg, width: AppSpacing.lg);
  static const Widget xl = SizedBox(height: AppSpacing.xl, width: AppSpacing.xl);
  static const Widget xxl = SizedBox(height: AppSpacing.xxl, width: AppSpacing.xxl);
}
