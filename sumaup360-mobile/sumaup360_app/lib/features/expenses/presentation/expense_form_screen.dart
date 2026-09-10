import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/category_picker.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../home/application/summary_providers.dart';
import '../application/expense_providers.dart';
import '../domain/expense.dart';

class ExpenseFormScreen extends ConsumerStatefulWidget {
  const ExpenseFormScreen({super.key});

  @override
  ConsumerState<ExpenseFormScreen> createState() => _ExpenseFormScreenState();
}

class _ExpenseFormScreenState extends ConsumerState<ExpenseFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _amount = TextEditingController();
  final _description = TextEditingController();
  DateTime _date = DateTime.now();
  String _category = expenseCategories.first;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final d = await showDatePicker(
      context: context, initialDate: _date, firstDate: DateTime(2020), lastDate: DateTime.now());
    if (d != null) setState(() => _date = d);
  }

  Future<void> _save() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await ref.read(expenseRepositoryProvider).create(
            txDate: _date,
            amount: double.parse(_amount.text.replaceAll(',', '.')),
            category: _category,
            description: _description.text.trim().isEmpty ? null : _description.text.trim(),
          );
      ref.invalidate(expenseListProvider);
      ref.invalidate(summaryProvider);
      if (mounted) context.pop();
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Registrar gasto')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: AppSpacing.screen,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AppTextField(
                  label: 'Monto',
                  controller: _amount,
                  hint: '0.00',
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  prefixIcon: 'dollar',
                  validator: (v) {
                    if ((v ?? '').trim().isEmpty) return 'Ingresa el monto';
                    if (double.tryParse((v ?? '').replaceAll(',', '.')) == null) return 'Monto invalido';
                    return null;
                  },
                ),
                const SizedBox(height: AppSpacing.lg),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Fecha', style: TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(height: AppSpacing.sm),
                    InkWell(
                      onTap: _pickDate,
                      borderRadius: BorderRadius.circular(AppSpacing.radius),
                      child: InputDecorator(
                        decoration: const InputDecoration(
                          prefixIcon: Padding(
                            padding: EdgeInsets.all(12),
                            child: AppIcon('calendar-event', size: 20, color: AppColors.muted),
                          ),
                          prefixIconConstraints: BoxConstraints(minWidth: 0, minHeight: 0),
                        ),
                        child: Text(Fmt.date(_date)),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.lg),
                CategoryPicker(
                  label: 'Categoria',
                  categories: expenseCategories,
                  selected: _category,
                  onSelected: (c) => setState(() => _category = c),
                ),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Descripcion (opcional)', controller: _description, prefixIcon: 'notes'),
                if (_error != null) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(_error!, style: const TextStyle(color: AppColors.danger)),
                ],
                const SizedBox(height: AppSpacing.xxl),
                PrimaryButton(label: 'Guardar gasto', loading: _loading, onPressed: _save),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
