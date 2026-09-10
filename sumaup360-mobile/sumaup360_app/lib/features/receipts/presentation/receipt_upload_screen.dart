import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/category_picker.dart';
import '../../../shared/widgets/primary_button.dart';
import '../application/receipt_providers.dart';
import '../domain/receipt.dart';

class ReceiptUploadScreen extends ConsumerStatefulWidget {
  const ReceiptUploadScreen({super.key});

  @override
  ConsumerState<ReceiptUploadScreen> createState() => _ReceiptUploadScreenState();
}

class _ReceiptUploadScreenState extends ConsumerState<ReceiptUploadScreen> {
  final _formKey = GlobalKey<FormState>();
  final _docNumber = TextEditingController();
  final _amount = TextEditingController();
  final _provider = TextEditingController();
  final _notes = TextEditingController();
  DateTime _date = DateTime.now();
  String _docType = receiptDocTypes.first;
  File? _file;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _docNumber.dispose();
    _amount.dispose();
    _provider.dispose();
    _notes.dispose();
    super.dispose();
  }

  Future<void> _pick(ImageSource source) async {
    try {
      final x = await ImagePicker().pickImage(source: source, imageQuality: 70, maxWidth: 1600);
      if (x != null) setState(() => _file = File(x.path));
    } catch (_) {
      setState(() => _error = 'No se pudo abrir la camara/galeria.');
    }
  }

  Future<void> _pickDate() async {
    final d = await showDatePicker(
      context: context, initialDate: _date, firstDate: DateTime(2020), lastDate: DateTime.now());
    if (d != null) setState(() => _date = d);
  }

  Future<void> _save() async {
    setState(() => _error = null);
    if (_file == null) {
      setState(() => _error = 'Toma o elige una foto del comprobante.');
      return;
    }
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      final repo = ref.read(receiptRepositoryProvider);
      final path = await repo.uploadFile(_file!);
      final notes = [
        _docType,
        if (_provider.text.trim().isNotEmpty) 'Proveedor: ${_provider.text.trim()}',
        if (_notes.text.trim().isNotEmpty) _notes.text.trim(),
      ].join(' · ');
      await repo.create(
        docNumber: _docNumber.text.trim().isEmpty ? null : _docNumber.text.trim(),
        issueDate: _date,
        amount: _amount.text.trim().isEmpty ? null : double.tryParse(_amount.text.replaceAll(',', '.')),
        fileUrl: path,
        notes: notes,
      );
      ref.invalidate(receiptListProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Comprobante subido.')));
        context.pop();
      }
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (e) {
      setState(() => _error = 'No se pudo subir: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Subir comprobante')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: AppSpacing.screen,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Preview(file: _file, onCamera: () => _pick(ImageSource.camera), onGallery: () => _pick(ImageSource.gallery)),
                const SizedBox(height: AppSpacing.md),
                OutlinedButton.icon(
                  onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Deteccion con IA: disponible pronto.')),
                  ),
                  icon: const AppIcon('magic', size: 18),
                  label: const Text('Detectar datos con IA'),
                ),
                const SizedBox(height: AppSpacing.lg),
                CategoryPicker(
                  label: 'Tipo de documento',
                  categories: receiptDocTypes,
                  selected: _docType,
                  onSelected: (c) => setState(() => _docType = c),
                ),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Numero de documento (opcional)', controller: _docNumber, prefixIcon: 'tag'),
                const SizedBox(height: AppSpacing.lg),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Fecha de emision', style: TextStyle(fontWeight: FontWeight.w600)),
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
                AppTextField(label: 'Nombre del proveedor (opcional)', controller: _provider, prefixIcon: 'store'),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(
                  label: 'Total (opcional)',
                  controller: _amount,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  prefixIcon: 'dollar',
                ),
                const SizedBox(height: AppSpacing.lg),
                AppTextField(label: 'Notas (opcional)', controller: _notes, prefixIcon: 'notes'),
                if (_error != null) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(_error!, style: const TextStyle(color: AppColors.danger)),
                ],
                const SizedBox(height: AppSpacing.xxl),
                PrimaryButton(label: 'Guardar comprobante', loading: _loading, onPressed: _save),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Preview extends StatelessWidget {
  const _Preview({required this.file, required this.onCamera, required this.onGallery});
  final File? file;
  final VoidCallback onCamera;
  final VoidCallback onGallery;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 200,
      decoration: BoxDecoration(
        color: AppColors.surfaceAlt,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        border: Border.all(color: AppColors.border),
      ),
      clipBehavior: Clip.antiAlias,
      child: file != null
          ? Stack(
              fit: StackFit.expand,
              children: [
                Image.file(file!, fit: BoxFit.cover),
                Positioned(
                  right: 8, bottom: 8,
                  child: Row(children: [
                    _MiniBtn(icon: 'camera', onTap: onCamera),
                    const SizedBox(width: 8),
                    _MiniBtn(icon: 'photo', onTap: onGallery),
                  ]),
                ),
              ],
            )
          : Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const AppIcon('receipt-tax', size: 44, color: AppColors.muted),
                const SizedBox(height: 6),
                const Text('Agrega la foto del comprobante', style: AppText.small),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    TextButton.icon(onPressed: onCamera, icon: const AppIcon('camera', size: 20), label: const Text('Camara')),
                    TextButton.icon(onPressed: onGallery, icon: const AppIcon('photo', size: 20), label: const Text('Galeria')),
                  ],
                ),
              ],
            ),
    );
  }
}

class _MiniBtn extends StatelessWidget {
  const _MiniBtn({required this.icon, required this.onTap});
  final String icon;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: CircleAvatar(radius: 18, backgroundColor: Colors.black54, child: AppIcon(icon, size: 18, color: Colors.white)),
    );
  }
}
