import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/constants/app_colors.dart';
import 'app_icon.dart';

/// Boton de retroceso circular y discreto.
class AppBackButton extends StatelessWidget {
  const AppBackButton({super.key, this.onTap});
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onTap ?? () => context.pop(),
      icon: const AppIcon('square-rounded-arrow-left', size: 20, color: AppColors.ink),
      style: IconButton.styleFrom(
        backgroundColor: AppColors.surfaceAlt,
        shape: const CircleBorder(),
      ),
    );
  }
}
