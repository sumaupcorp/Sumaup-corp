import 'package:flutter/material.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_text_styles.dart';
import 'app_icon.dart';

/// Titulo de seccion con icono de informacion opcional (?) y accion opcional.
///
/// - [info]: si se pasa, muestra el icono help-small que abre una hoja
///   inferior explicando la seccion.
/// - [action] / [onAction]: enlace a la derecha (ej. "Ver todo").
class InfoTitle extends StatelessWidget {
  const InfoTitle(
    this.title, {
    super.key,
    this.info,
    this.action,
    this.onAction,
  });

  final String title;
  final String? info;
  final String? action;
  final VoidCallback? onAction;

  void _showInfo(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => Padding(
        padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, AppSpacing.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(color: AppColors.border, borderRadius: BorderRadius.circular(2)),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(10)),
                  alignment: Alignment.center,
                  child: const AppIcon('help-small', size: 22, color: AppColors.primary),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(child: Text(title, style: AppText.title)),
              ],
            ),
            const SizedBox(height: AppSpacing.md),
            Text(info!, style: AppText.body),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Flexible(child: Text(title, style: AppText.title, overflow: TextOverflow.ellipsis)),
              if (info != null) ...[
                const SizedBox(width: 6),
                GestureDetector(
                  onTap: () => _showInfo(context),
                  child: const Padding(
                    padding: EdgeInsets.all(2),
                    child: AppIcon('help-small', size: 18, color: AppColors.muted),
                  ),
                ),
              ],
            ],
          ),
        ),
        if (action != null)
          GestureDetector(
            onTap: onAction,
            child: Text(action!, style: AppText.small.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600)),
          ),
      ],
    );
  }
}
