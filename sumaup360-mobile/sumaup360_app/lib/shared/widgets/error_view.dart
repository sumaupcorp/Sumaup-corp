import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_assets.dart';
import '../../core/constants/app_text_styles.dart';
import 'primary_button.dart';
import 'suma_image.dart';

/// Estado de error amigable (con Suma) y opcion de reintentar.
class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SumaImage(AppAssets.sumaError, height: 150),
            const SizedBox(height: 16),
            const Text('Algo salio mal', style: AppText.h2),
            const SizedBox(height: 6),
            Text(message, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.body)),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              SizedBox(width: 200, child: PrimaryButton(label: 'Reintentar', onPressed: onRetry)),
            ],
          ],
        ),
      ),
    );
  }
}
