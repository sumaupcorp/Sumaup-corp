import 'package:flutter/material.dart';
import 'app_colors.dart';

/// Estilos de texto de la app. Tipografia de marca: Poppins.
class AppText {
  AppText._();

  static const String _family = 'Poppins';

  static const TextStyle display = TextStyle(
    fontFamily: _family, fontSize: 28, fontWeight: FontWeight.w700, color: AppColors.ink, height: 1.2);

  static const TextStyle h1 = TextStyle(
    fontFamily: _family, fontSize: 24, fontWeight: FontWeight.w700, color: AppColors.ink, height: 1.25);

  static const TextStyle h2 = TextStyle(
    fontFamily: _family, fontSize: 20, fontWeight: FontWeight.w700, color: AppColors.ink);

  static const TextStyle title = TextStyle(
    fontFamily: _family, fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.ink);

  static const TextStyle body = TextStyle(
    fontFamily: _family, fontSize: 15, fontWeight: FontWeight.w400, color: AppColors.body, height: 1.45);

  static const TextStyle bodyStrong = TextStyle(
    fontFamily: _family, fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.ink);

  static const TextStyle small = TextStyle(
    fontFamily: _family, fontSize: 13, fontWeight: FontWeight.w400, color: AppColors.muted);

  static const TextStyle button = TextStyle(
    fontFamily: _family, fontSize: 16, fontWeight: FontWeight.w600);
}
