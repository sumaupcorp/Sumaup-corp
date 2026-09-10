import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';

/// Selector de categoria por chips.
class CategoryPicker extends StatelessWidget {
  const CategoryPicker({
    super.key,
    required this.label,
    required this.categories,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final List<String> categories;
  final String selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: AppSpacing.sm),
        Wrap(
          spacing: AppSpacing.sm,
          runSpacing: AppSpacing.sm,
          children: categories.map((c) {
            final active = c == selected;
            return ChoiceChip(
              label: Text(c),
              selected: active,
              onSelected: (_) => onSelected(c),
              showCheckmark: false,
              labelStyle: TextStyle(color: active ? Colors.white : AppColors.body, fontWeight: FontWeight.w500),
              selectedColor: AppColors.primary,
              backgroundColor: AppColors.surfaceAlt,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                side: BorderSide(color: active ? AppColors.primary : AppColors.border),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}
