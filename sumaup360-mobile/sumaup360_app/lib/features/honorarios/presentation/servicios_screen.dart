import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_icon.dart';
import '../application/honorarios_providers.dart';
import '../domain/honorarios_models.dart';

/// Modulo Servicios Profesionales (Premium): Recibos por Honorarios + Suspension de 4ta.
class ServiciosScreen extends ConsumerWidget {
  const ServiciosScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final honorarios = ref.watch(honorariosProvider);
    final suspensiones = ref.watch(suspensionesProvider);

    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(honorariosProvider);
        ref.invalidate(suspensionesProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!embedded) ...[const Text('Servicios', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],

          // --- Recibos por Honorarios ---
          const Text('Recibos por Honorarios', style: AppText.title),
          const SizedBox(height: AppSpacing.md),
          _HonorarioCta(onTap: () => _openHonorarioForm(context, ref)),
          const SizedBox(height: AppSpacing.md),
          honorarios.when(
            loading: () => const _Loading(),
            error: (_, __) => const Text('No pudimos cargar tus recibos.', style: AppText.small),
            data: (list) => list.isEmpty
                ? const _Empty(text: 'Aun no solicitas ningun recibo.')
                : Column(children: list.map((h) => _HonorarioTile(item: h)).toList()),
          ),

          const SizedBox(height: AppSpacing.xl),

          // --- Suspension de 4ta ---
          const Text('Suspension de 4ta categoria', style: AppText.title),
          const SizedBox(height: AppSpacing.md),
          _SuspensionCta(onGenerate: (anio) => _createSuspension(context, ref, anio)),
          const SizedBox(height: AppSpacing.md),
          suspensiones.when(
            loading: () => const _Loading(),
            error: (_, __) => const Text('No pudimos cargar tus solicitudes.', style: AppText.small),
            data: (list) => list.isEmpty
                ? const _Empty(text: 'Aun no solicitas ninguna suspension.')
                : Column(children: list.map((s) => _SuspensionTile(item: s)).toList()),
          ),
        ],
      ),
    );

    if (embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Servicios')),
      body: SafeArea(child: body),
    );
  }

  void _openHonorarioForm(BuildContext context, WidgetRef ref) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => _HonorarioForm(
        onSubmit: (data) async {
          final repo = ref.read(honorariosRepositoryProvider);
          await repo.createHonorario(
            clienteNombre: data.clienteNombre,
            clienteDocType: data.docType,
            clienteDocNumber: data.docNumber,
            descripcion: data.descripcion,
            monto: data.monto,
            conRetencion: data.conRetencion,
          );
          ref.invalidate(honorariosProvider);
        },
      ),
    );
  }

  Future<void> _createSuspension(BuildContext context, WidgetRef ref, int anio) async {
    try {
      await ref.read(honorariosRepositoryProvider).createSuspension(anio);
      ref.invalidate(suspensionesProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Solicitud enviada. SUMAUP360 tramitara tu suspension.')),
        );
      }
    } on AppException catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }
}

// --- Recibos: CTA + formulario ---

class _HonorarioCta extends StatelessWidget {
  const _HonorarioCta({required this.onTap});
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Solicita tu recibo por honorarios', style: AppText.bodyStrong),
          const SizedBox(height: 4),
          const Text('Completa los datos del servicio y SUMAUP360 genera tu recibo.', style: AppText.small),
          const SizedBox(height: AppSpacing.lg),
          SizedBox(
            height: 50,
            child: ElevatedButton(
              onPressed: onTap,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
              ),
              child: const Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                AppIcon('notes', size: 20, color: Colors.white),
                SizedBox(width: 8),
                Text('Solicitar recibo', style: AppText.button),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}

class _HonorarioFormData {
  _HonorarioFormData(this.clienteNombre, this.docType, this.docNumber, this.descripcion, this.monto, this.conRetencion);
  final String clienteNombre;
  final String? docType;
  final String? docNumber;
  final String descripcion;
  final double monto;
  final bool conRetencion;
}

class _HonorarioForm extends StatefulWidget {
  const _HonorarioForm({required this.onSubmit});
  final Future<void> Function(_HonorarioFormData) onSubmit;

  @override
  State<_HonorarioForm> createState() => _HonorarioFormState();
}

class _HonorarioFormState extends State<_HonorarioForm> {
  final _formKey = GlobalKey<FormState>();
  final _nombre = TextEditingController();
  final _docNumber = TextEditingController();
  final _descripcion = TextEditingController();
  final _monto = TextEditingController();
  String _docType = 'RUC';
  bool _conRetencion = false;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _nombre.dispose();
    _docNumber.dispose();
    _descripcion.dispose();
    _monto.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    final monto = double.tryParse(_monto.text.replaceAll(',', '.'));
    if (monto == null || monto <= 0) {
      setState(() => _error = 'Ingresa un monto valido.');
      return;
    }
    setState(() => _loading = true);
    try {
      await widget.onSubmit(_HonorarioFormData(
        _nombre.text.trim(),
        _docType,
        _docNumber.text.trim().isEmpty ? null : _docNumber.text.trim(),
        _descripcion.text.trim(),
        monto,
        _conRetencion,
      ));
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Solicitud enviada. SUMAUP360 generara tu recibo.')),
        );
      }
    } on AppException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'No se pudo enviar la solicitud.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Nuevo recibo por honorarios', style: AppText.title),
              const SizedBox(height: AppSpacing.lg),
              TextFormField(
                controller: _nombre,
                decoration: const InputDecoration(labelText: 'Nombre o razon social del cliente'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Requerido' : null,
              ),
              const SizedBox(height: AppSpacing.md),
              Row(
                children: [
                  SizedBox(
                    width: 110,
                    child: DropdownButtonFormField<String>(
                      initialValue: _docType,
                      decoration: const InputDecoration(labelText: 'Doc.'),
                      items: const [
                        DropdownMenuItem(value: 'RUC', child: Text('RUC')),
                        DropdownMenuItem(value: 'DNI', child: Text('DNI')),
                      ],
                      onChanged: (v) => setState(() => _docType = v ?? 'RUC'),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: TextFormField(
                      controller: _docNumber,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Numero'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              TextFormField(
                controller: _descripcion,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Descripcion del servicio prestado'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Requerido' : null,
              ),
              const SizedBox(height: AppSpacing.md),
              TextFormField(
                controller: _monto,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'Monto (S/)', prefixText: 'S/ '),
              ),
              const SizedBox(height: AppSpacing.sm),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Con retencion de 4ta', style: AppText.body),
                value: _conRetencion,
                activeThumbColor: AppColors.primary,
                onChanged: (v) => setState(() => _conRetencion = v),
              ),
              if (_error != null) ...[
                const SizedBox(height: AppSpacing.sm),
                Text(_error!, style: const TextStyle(color: AppColors.danger)),
              ],
              const SizedBox(height: AppSpacing.lg),
              SizedBox(
                height: 50,
                child: ElevatedButton(
                  onPressed: _loading ? null : _submit,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                  ),
                  child: _loading
                      ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                      : const Text('Enviar solicitud', style: AppText.button),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HonorarioTile extends StatelessWidget {
  const _HonorarioTile({required this.item});
  final HonorarioRequestModel item;

  @override
  Widget build(BuildContext context) {
    final h = item;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(h.clienteNombre ?? 'Cliente', style: AppText.bodyStrong)),
              _HonorarioBadge(estado: h.estado),
            ],
          ),
          const SizedBox(height: 4),
          Text('${h.descripcion ?? ''}  ·  S/ ${(h.monto ?? 0).toStringAsFixed(2)}', style: AppText.small),
          if ((h.observacion ?? '').isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(h.observacion!, style: AppText.small.copyWith(color: AppColors.warning)),
          ],
          if ((h.reciboUrl ?? '').isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            _DownloadLink(url: h.reciboUrl!, label: 'Descargar recibo'),
          ],
        ],
      ),
    );
  }
}

// --- Suspension: CTA + tile ---

class _SuspensionCta extends StatefulWidget {
  const _SuspensionCta({required this.onGenerate});
  final Future<void> Function(int anio) onGenerate;

  @override
  State<_SuspensionCta> createState() => _SuspensionCtaState();
}

class _SuspensionCtaState extends State<_SuspensionCta> {
  late int _anio;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _anio = DateTime.now().year;
  }

  @override
  Widget build(BuildContext context) {
    final years = [DateTime.now().year, DateTime.now().year + 1];
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Solicita tu suspension de 4ta', style: AppText.bodyStrong),
          const SizedBox(height: 4),
          const Text('Elige el año y SUMAUP360 la tramita ante SUNAT por ti.', style: AppText.small),
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
                decoration: BoxDecoration(
                  color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
                child: DropdownButton<int>(
                  value: _anio,
                  underline: const SizedBox.shrink(),
                  items: years.map((y) => DropdownMenuItem(value: y, child: Text('Año $y'))).toList(),
                  onChanged: (v) => setState(() => _anio = v ?? _anio),
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: SizedBox(
                  height: 50,
                  child: ElevatedButton(
                    onPressed: _loading ? null : () async {
                      setState(() => _loading = true);
                      await widget.onGenerate(_anio);
                      if (mounted) setState(() => _loading = false);
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary, foregroundColor: Colors.white, elevation: 0,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                    ),
                    child: _loading
                        ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                        : const Text('Generar solicitud', style: AppText.button),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SuspensionTile extends StatelessWidget {
  const _SuspensionTile({required this.item});
  final SuspensionRequestModel item;

  @override
  Widget build(BuildContext context) {
    final s = item;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface, borderRadius: BorderRadius.circular(AppSpacing.radiusLg), boxShadow: AppShadows.card),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text('Suspension ${s.anio ?? ''}', style: AppText.bodyStrong)),
              _SuspensionBadge(estado: s.estado),
            ],
          ),
          if ((s.observacion ?? '').isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(s.observacion!, style: AppText.small.copyWith(color: AppColors.warning)),
          ],
          if ((s.constanciaUrl ?? '').isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            _DownloadLink(url: s.constanciaUrl!, label: 'Descargar constancia'),
          ],
        ],
      ),
    );
  }
}

// --- comunes ---

class _DownloadLink extends StatelessWidget {
  const _DownloadLink({required this.url, required this.label});
  final String url;
  final String label;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        Clipboard.setData(ClipboardData(text: url));
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enlace copiado.')));
      },
      child: Row(children: [
        const AppIcon('download', size: 16, color: AppColors.primary),
        const SizedBox(width: 6),
        Text(label, style: AppText.small.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600)),
      ]),
    );
  }
}

class _Loading extends StatelessWidget {
  const _Loading();
  @override
  Widget build(BuildContext context) => const Center(
        child: Padding(padding: EdgeInsets.all(AppSpacing.xl), child: CircularProgressIndicator(color: AppColors.primary)),
      );
}

class _Empty extends StatelessWidget {
  const _Empty({required this.text});
  final String text;
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpacing.xl),
        decoration: BoxDecoration(color: AppColors.surfaceAlt, borderRadius: BorderRadius.circular(AppSpacing.radius)),
        child: Text(text, textAlign: TextAlign.center, style: AppText.small),
      );
}

class _HonorarioBadge extends StatelessWidget {
  const _HonorarioBadge({required this.estado});
  final String? estado;
  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (estado) {
      'PENDIENTE' => ('Pendiente', AppColors.primary),
      'EN_PROCESO' => ('En proceso', AppColors.accent),
      'GENERADO' => ('Generado', AppColors.success),
      'OBSERVADO' => ('Observado', AppColors.warning),
      'CANCELADO' => ('Cancelado', AppColors.danger),
      _ => (estado ?? '—', AppColors.muted),
    };
    return _Pill(label: label, color: color);
  }
}

class _SuspensionBadge extends StatelessWidget {
  const _SuspensionBadge({required this.estado});
  final String? estado;
  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (estado) {
      'SOLICITADA' => ('Solicitada', AppColors.primary),
      'EN_PROCESO' => ('En proceso', AppColors.accent),
      'TRAMITADA' => ('Tramitada', AppColors.success),
      'OBSERVADA' => ('Observada', AppColors.warning),
      'CANCELADA' => ('Cancelada', AppColors.danger),
      _ => (estado ?? '—', AppColors.muted),
    };
    return _Pill(label: label, color: color);
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.label, required this.color});
  final String label;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(AppSpacing.radiusPill)),
        child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700)),
      );
}
