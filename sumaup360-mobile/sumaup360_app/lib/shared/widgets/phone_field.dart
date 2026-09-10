import 'package:country_flags/country_flags.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/constants/app_colors.dart';
import '../../core/constants/app_spacing.dart';
import '../../core/constants/app_text_styles.dart';
import 'app_icon.dart';

/// Pais con su codigo ISO y prefijo telefonico.
class Country {
  const Country(this.iso, this.dial, this.name);
  final String iso; // 'PE'
  final String dial; // '+51'
  final String name; // 'Peru'
}

/// Lista de paises soportados (Peru por defecto + LATAM y comunes).
const kCountries = <Country>[
  Country('PE', '+51', 'Peru'),
  Country('AR', '+54', 'Argentina'),
  Country('BO', '+591', 'Bolivia'),
  Country('BR', '+55', 'Brasil'),
  Country('CL', '+56', 'Chile'),
  Country('CO', '+57', 'Colombia'),
  Country('CR', '+506', 'Costa Rica'),
  Country('EC', '+593', 'Ecuador'),
  Country('SV', '+503', 'El Salvador'),
  Country('GT', '+502', 'Guatemala'),
  Country('MX', '+52', 'Mexico'),
  Country('PA', '+507', 'Panama'),
  Country('PY', '+595', 'Paraguay'),
  Country('UY', '+598', 'Uruguay'),
  Country('VE', '+58', 'Venezuela'),
  Country('US', '+1', 'Estados Unidos'),
  Country('ES', '+34', 'Espana'),
];

/// Formatea los digitos en grupos de 3 separados por guion: 000-000-000.
class _PhoneDigitsFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    var digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    if (digits.length > 11) digits = digits.substring(0, 11);
    final buf = StringBuffer();
    for (var i = 0; i < digits.length; i++) {
      if (i != 0 && i % 3 == 0) buf.write('-');
      buf.write(digits[i]);
    }
    final text = buf.toString();
    return TextEditingValue(text: text, selection: TextSelection.collapsed(offset: text.length));
  }
}

/// Campo de telefono con selector de pais y bandera.
/// Formato visual: [PE +51] | 000-000-000
class PhoneField extends StatefulWidget {
  const PhoneField({
    super.key,
    required this.controller,
    this.label = 'Celular',
    this.initialIso = 'PE',
    this.onCountryChanged,
    this.validator,
  });

  final TextEditingController controller;
  final String label;
  final String initialIso;
  final ValueChanged<Country>? onCountryChanged;
  final String? Function(String?)? validator;

  @override
  State<PhoneField> createState() => _PhoneFieldState();
}

class _PhoneFieldState extends State<PhoneField> {
  late Country _country;

  @override
  void initState() {
    super.initState();
    _country = kCountries.firstWhere(
      (c) => c.iso == widget.initialIso,
      orElse: () => kCountries.first,
    );
  }

  Future<void> _pickCountry() async {
    final selected = await showModalBottomSheet<Country>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => const _CountrySheet(),
    );
    if (selected != null) {
      setState(() => _country = selected);
      widget.onCountryChanged?.call(selected);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(widget.label, style: AppText.small.copyWith(color: AppColors.body, fontWeight: FontWeight.w600)),
        const SizedBox(height: 6),
        Container(
          decoration: BoxDecoration(
            color: AppColors.surfaceAlt,
            borderRadius: BorderRadius.circular(AppSpacing.radius),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              // Selector de pais
              InkWell(
                onTap: _pickCountry,
                borderRadius: BorderRadius.circular(AppSpacing.radius),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 14),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      CountryFlag.fromCountryCode(
                        _country.iso,
                        theme: const ImageTheme(width: 26, height: 18, shape: RoundedRectangle(3)),
                      ),
                      const SizedBox(width: 8),
                      Text(_country.iso, style: AppText.bodyStrong),
                      const SizedBox(width: 4),
                      Text(_country.dial, style: AppText.body.copyWith(color: AppColors.body)),
                      const SizedBox(width: 2),
                      const AppIcon('chevron-down', size: 16, color: AppColors.muted),
                    ],
                  ),
                ),
              ),
              Container(width: 1, height: 28, color: AppColors.border),
              // Numero
              Expanded(
                child: TextFormField(
                  controller: widget.controller,
                  keyboardType: TextInputType.phone,
                  inputFormatters: [_PhoneDigitsFormatter()],
                  validator: widget.validator,
                  style: AppText.body,
                  decoration: const InputDecoration(
                    hintText: '000-000-000',
                    hintStyle: TextStyle(color: AppColors.muted),
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    errorBorder: InputBorder.none,
                    focusedErrorBorder: InputBorder.none,
                    contentPadding: EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 14),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CountrySheet extends StatefulWidget {
  const _CountrySheet();

  @override
  State<_CountrySheet> createState() => _CountrySheetState();
}

class _CountrySheetState extends State<_CountrySheet> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final filtered = kCountries.where((c) {
      final q = _query.toLowerCase();
      return q.isEmpty || c.name.toLowerCase().contains(q) || c.dial.contains(q) || c.iso.toLowerCase().contains(q);
    }).toList();

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: AppSpacing.md),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(color: AppColors.border, borderRadius: BorderRadius.circular(2)),
            ),
            const SizedBox(height: AppSpacing.md),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: TextField(
                autofocus: false,
                onChanged: (v) => setState(() => _query = v),
                decoration: InputDecoration(
                  hintText: 'Buscar pais',
                  prefixIcon: const Padding(
                    padding: EdgeInsets.all(12),
                    child: AppIcon('search', size: 20, color: AppColors.muted),
                  ),
                  prefixIconConstraints: const BoxConstraints(minWidth: 0, minHeight: 0),
                  filled: true,
                  fillColor: AppColors.surfaceAlt,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radius),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Flexible(
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: filtered.length,
                itemBuilder: (_, i) {
                  final c = filtered[i];
                  return ListTile(
                    leading: CountryFlag.fromCountryCode(
                      c.iso,
                      theme: const ImageTheme(width: 30, height: 22, shape: RoundedRectangle(3)),
                    ),
                    title: Text(c.name, style: AppText.bodyStrong),
                    trailing: Text(c.dial, style: AppText.body.copyWith(color: AppColors.body)),
                    onTap: () => Navigator.of(context).pop(c),
                  );
                },
              ),
            ),
            const SizedBox(height: AppSpacing.md),
          ],
        ),
      ),
    );
  }
}
