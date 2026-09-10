import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_text_styles.dart';

/// Tarjeta de total (ingresos/gastos del mes).
class MoneyTotalCard extends StatelessWidget {
  const MoneyTotalCard({super.key, required this.label, required this.value, required this.color});
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: [color.withValues(alpha: 0.12), AppColors.surfaceAlt]),
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: AppText.small),
          const SizedBox(height: 6),
          Text(value, style: AppText.display.copyWith(color: color)),
        ],
      ),
    );
  }
}

/// Estado vacio compacto.
class EmptyHint extends StatelessWidget {
  const EmptyHint({super.key, required this.text});
  final String text;
  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xxl),
      decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
      child: Text(text, textAlign: TextAlign.center, style: AppText.small),
    );
  }
}
