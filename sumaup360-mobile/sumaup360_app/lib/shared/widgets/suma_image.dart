import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';
import 'app_icon.dart';

/// Muestra una imagen de la mascota Suma con fallback visual si el asset no existe.
class SumaImage extends StatelessWidget {
  const SumaImage(this.asset, {super.key, this.height = 200});

  final String asset;
  final double height;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      asset,
      height: height,
      fit: BoxFit.contain,
      errorBuilder: (_, __, ___) => Container(
        height: height,
        width: height,
        decoration: const BoxDecoration(
          color: AppColors.primarySoft,
          shape: BoxShape.circle,
        ),
        alignment: Alignment.center,
        child: const AppIcon('photo', size: 56, color: AppColors.primary),
      ),
    );
  }
}
