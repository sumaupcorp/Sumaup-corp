import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/info_title.dart';
import '../../../shared/widgets/money_total_card.dart';
import '../application/taxi_providers.dart';
import '../domain/corporate_ride.dart';
import '../domain/receipt_request.dart';
import 'receipt_detail_screen.dart';
import 'widgets/taxi_qr_card.dart';

/// Modulo Corporativo del taxista (Premium). Reemplaza la vista de gastos.
///
/// Unifica los dos flujos de cobro del taxista:
///  - Corporativo: carreras pagadas con tarjeta en la app de la central
///    (el dinero fue a la empresa) -> el taxista le factura a la empresa.
///  - Directo (QR): su QR unico para carreras directas, donde el pasajero
///    escanea, pide su comprobante y la carrera se registra automaticamente.
class CorporativoScreen extends ConsumerStatefulWidget {
  const CorporativoScreen({super.key, this.embedded = false});
  final bool embedded;

  @override
  ConsumerState<CorporativoScreen> createState() => _CorporativoScreenState();
}

class _CorporativoScreenState extends ConsumerState<CorporativoScreen> {
  int _segment = 0; // 0 = Corporativo, 1 = Directo (QR)
  final Set<String> _selected = <String>{};
  bool _submitting = false;

  void _toggle(String id) {
    setState(() {
      if (_selected.contains(id)) {
        _selected.remove(id);
      } else {
        _selected.add(id);
      }
    });
  }

  void _selectAll(List<CorporateRide> facturables) {
    setState(() {
      final all = facturables.map((r) => r.id).toSet();
      if (_selected.containsAll(all) && all.isNotEmpty) {
        _selected.clear();
      } else {
        _selected.addAll(all);
      }
    });
  }

  Future<void> _generarFactura(List<CorporateRide> facturables, String empresa) async {
    final ids = facturables.where((r) => _selected.contains(r.id)).map((r) => r.id).toList();
    if (ids.isEmpty) return;
    final total = facturables.where((r) => _selected.contains(r.id)).fold<double>(0, (s, r) => s + r.monto);

    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radiusLg)),
        title: const Text('Generar factura'),
        content: Text(
          'Se generara tu factura hacia $empresa por ${ids.length} '
          '${ids.length == 1 ? 'carrera' : 'carreras'} (${Fmt.money(total)}).',
          style: AppText.body,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Generar')),
        ],
      ),
    );
    if (ok != true || !mounted) return;

    setState(() => _submitting = true);
    try {
      await ref.read(taxiRepositoryProvider).createCorporateInvoice(ids);
      _selected.clear();
      ref.invalidate(corporateRidesProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Factura enviada a la empresa. Te avisaremos cuando te paguen.')),
        );
      }
    } on AppException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ridesAsync = ref.watch(corporateRidesProvider);
    final rides = ridesAsync.valueOrNull ?? const <CorporateRide>[];
    final facturables = rides.where((r) => r.facturable).toList();
    final historial = rides.where((r) => !r.facturable).toList();
    final porFacturar = facturables.fold<double>(0, (s, r) => s + r.monto);
    final facturado = historial.fold<double>(0, (s, r) => s + r.monto);
    final empresa = rides
            .map((r) => r.empresa)
            .firstWhere((e) => (e ?? '').trim().isNotEmpty, orElse: () => null) ??
        'tu central';

    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(corporateRidesProvider);
        ref.invalidate(qrTokenProvider);
        ref.invalidate(taxiRequestsProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!widget.embedded) ...[const Text('Corporativo', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],
          _HeroCard(
            porFacturar: porFacturar,
            countFacturar: facturables.length,
            facturado: facturado,
            empresa: empresa,
          ),
          const SizedBox(height: AppSpacing.lg),
          _SegmentedTabs(
            current: _segment,
            onChanged: (i) => setState(() => _segment = i),
          ),
          const SizedBox(height: AppSpacing.lg),
          if (_segment == 0)
            _CorporativoSection(
              ridesAsync: ridesAsync,
              facturables: facturables,
              historial: historial,
              selected: _selected,
              submitting: _submitting,
              empresa: empresa,
              onToggle: _toggle,
              onSelectAll: () => _selectAll(facturables),
              onGenerar: () => _generarFactura(facturables, empresa),
            )
          else
            const _DirectoSection(),
        ],
      ),
    );

    if (widget.embedded) return body;
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(title: const Text('Corporativo')),
      body: SafeArea(child: body),
    );
  }
}

/// Hero: dinero por facturar a la empresa (gradiente azul de marca).
class _HeroCard extends StatelessWidget {
  const _HeroCard({
    required this.porFacturar,
    required this.countFacturar,
    required this.facturado,
    required this.empresa,
  });

  final double porFacturar;
  final int countFacturar;
  final double facturado;
  final String empresa;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary, AppColors.primaryDark],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: AppShadows.primary,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('Por cobrar a $empresa',
                  style: AppText.small.copyWith(color: Colors.white.withValues(alpha: 0.85))),
              const Spacer(),
              AppIcon('receipt', size: 20, color: Colors.white.withValues(alpha: 0.9)),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(Fmt.money(porFacturar), style: AppText.display.copyWith(color: Colors.white, fontSize: 32)),
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              _HeroPill(
                icon: 'receipt-tax',
                label: 'Por facturar',
                value: '$countFacturar ${countFacturar == 1 ? 'carrera' : 'carreras'}',
              ),
              const SizedBox(width: AppSpacing.md),
              _HeroPill(icon: 'check-circle', label: 'Facturado', value: Fmt.money(facturado)),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeroPill extends StatelessWidget {
  const _HeroPill({required this.icon, required this.label, required this.value});
  final String icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(AppSpacing.radius),
        ),
        child: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), shape: BoxShape.circle),
              alignment: Alignment.center,
              child: AppIcon(icon, size: 18, color: Colors.white),
            ),
            const SizedBox(width: AppSpacing.sm),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.8))),
                  const SizedBox(height: 2),
                  Text(value,
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: Colors.white),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Control segmentado tipo pildora (Corporativo / Directo QR).
class _SegmentedTabs extends StatelessWidget {
  const _SegmentedTabs({required this.current, required this.onChanged});
  final int current;
  final ValueChanged<int> onChanged;

  static const _items = <(String, String)>[
    ('receipt-tax', 'Corporativo'),
    ('folder-share', 'Directo · QR'),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: AppColors.surfaceAlt,
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: List.generate(_items.length, (i) {
          final selected = i == current;
          final (icon, label) = _items[i];
          return Expanded(
            child: GestureDetector(
              onTap: () => onChanged(i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 180),
                padding: const EdgeInsets.symmetric(vertical: 11),
                decoration: BoxDecoration(
                  color: selected ? AppColors.surface : Colors.transparent,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                  boxShadow: selected ? AppShadows.card : null,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    AppIcon(icon, size: 18, color: selected ? AppColors.primary : AppColors.muted),
                    const SizedBox(width: 6),
                    Text(
                      label,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                        color: selected ? AppColors.primary : AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}

/// Segmento Corporativo: carreras por facturar + generar factura + historial.
class _CorporativoSection extends StatelessWidget {
  const _CorporativoSection({
    required this.ridesAsync,
    required this.facturables,
    required this.historial,
    required this.selected,
    required this.submitting,
    required this.empresa,
    required this.onToggle,
    required this.onSelectAll,
    required this.onGenerar,
  });

  final AsyncValue<List<CorporateRide>> ridesAsync;
  final List<CorporateRide> facturables;
  final List<CorporateRide> historial;
  final Set<String> selected;
  final bool submitting;
  final String empresa;
  final ValueChanged<String> onToggle;
  final VoidCallback onSelectAll;
  final VoidCallback onGenerar;

  @override
  Widget build(BuildContext context) {
    final selectedRides = facturables.where((r) => selected.contains(r.id)).toList();
    final selTotal = selectedRides.fold<double>(0, (s, r) => s + r.monto);
    final allSelected = facturables.isNotEmpty && selected.containsAll(facturables.map((r) => r.id).toSet());

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _InfoCard(
          icon: 'receipt-tax',
          title: 'Carreras pagadas con tarjeta',
          text:
              'Cuando el pasajero paga con tarjeta en la app, el dinero va directo a la central. '
              'Selecciona tus carreras y genera una factura para que la empresa te pague.',
        ),
        const SizedBox(height: AppSpacing.lg),
        Row(
          children: [
            const Expanded(child: InfoTitle('Por facturar')),
            if (facturables.isNotEmpty)
              GestureDetector(
                onTap: onSelectAll,
                child: Text(
                  allSelected ? 'Quitar todas' : 'Seleccionar todas',
                  style: AppText.small.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                ),
              ),
          ],
        ),
        const SizedBox(height: AppSpacing.md),
        ridesAsync.when(
          loading: () => const _SectionLoader(),
          error: (_, __) => const _SectionError(),
          data: (_) => facturables.isEmpty
              ? const EmptyHint(text: 'No tienes carreras por facturar en este momento.')
              : Column(
                  children: facturables
                      .map((r) => _RideCard(
                            ride: r,
                            selectable: true,
                            selected: selected.contains(r.id),
                            onTap: () => onToggle(r.id),
                          ))
                      .toList(),
                ),
        ),
        if (selectedRides.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.sm),
          _GenerarBar(
            count: selectedRides.length,
            total: selTotal,
            submitting: submitting,
            onGenerar: onGenerar,
          ),
        ],
        if (historial.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.xl),
          const InfoTitle('Historial'),
          const SizedBox(height: AppSpacing.md),
          Column(children: historial.map((r) => _RideCard(ride: r)).toList()),
        ],
      ],
    );
  }
}

/// Segmento Directo (QR): QR unico + carreras registradas por el QR.
class _DirectoSection extends ConsumerWidget {
  const _DirectoSection();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final requests = ref.watch(taxiRequestsProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const TaxiQrCard(),
        const SizedBox(height: AppSpacing.xl),
        const InfoTitle(
          'Carreras registradas por QR',
          info: 'Cada vez que un pasajero escanea tu QR y pide su comprobante, la carrera aparece aqui.',
        ),
        const SizedBox(height: AppSpacing.md),
        requests.when(
          loading: () => const _SectionLoader(),
          error: (_, __) => const _SectionError(),
          data: (list) => list.isEmpty
              ? const EmptyHint(text: 'Aun no registras carreras por QR. Comparte tu QR con tus pasajeros.')
              : Column(children: list.map((r) => _RequestTile(request: r)).toList()),
        ),
      ],
    );
  }
}

/// Tarjeta de una carrera corporativa (seleccionable en modo facturar).
class _RideCard extends StatelessWidget {
  const _RideCard({required this.ride, this.selectable = false, this.selected = false, this.onTap});
  final CorporateRide ride;
  final bool selectable;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final (estadoLabel, estadoColor) = _estado(ride.estado);
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
        border: selected ? Border.all(color: AppColors.primary, width: 1.5) : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Row(
              children: [
                if (selectable) ...[
                  _Check(selected: selected),
                  const SizedBox(width: AppSpacing.md),
                ] else ...[
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(12)),
                    alignment: Alignment.center,
                    child: const AppIcon('receipt', size: 20, color: AppColors.primary),
                  ),
                  const SizedBox(width: AppSpacing.md),
                ],
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(ride.rutaLabel, style: AppText.bodyStrong, maxLines: 1, overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 2),
                      Text(
                        [Fmt.date(ride.fecha), if ((ride.medioPago ?? '').isNotEmpty) 'Tarjeta']
                            .where((e) => e.isNotEmpty)
                            .join(' · '),
                        style: AppText.small,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(Fmt.money(ride.monto), style: AppText.bodyStrong),
                    const SizedBox(height: 4),
                    _Badge(label: estadoLabel, color: estadoColor),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  (String, Color) _estado(String? e) {
    switch (e) {
      case 'FACTURADA':
        return ('Facturada', AppColors.accent);
      case 'PAGADA':
        return ('Pagada', AppColors.success);
      case 'RECHAZADA':
        return ('Rechazada', AppColors.danger);
      case 'PENDIENTE_FACTURA':
      default:
        return ('Por facturar', AppColors.warning);
    }
  }
}

/// Tile compacto de una solicitud/carrera registrada por QR.
class _RequestTile extends StatelessWidget {
  const _RequestTile({required this.request});
  final ReceiptRequestModel request;

  @override
  Widget build(BuildContext context) {
    final r = request;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => ReceiptDetailScreen(request: r)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(12)),
                  alignment: Alignment.center,
                  child: const AppIcon('receipt-tax', size: 20, color: AppColors.primary),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${r.tipo ?? 'Comprobante'} · ${Fmt.money(r.montoFinal)}', style: AppText.bodyStrong),
                      const SizedBox(height: 2),
                      Text(
                        [r.customerName, r.docNumber].where((e) => (e ?? '').isNotEmpty).join(' · ').isEmpty
                            ? 'Pasajero'
                            : [r.customerName, r.docNumber].where((e) => (e ?? '').isNotEmpty).join(' · '),
                        style: AppText.small,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                _RequestBadge(estado: r.estado),
                const SizedBox(width: 6),
                const Icon(Icons.chevron_right, color: AppColors.muted, size: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Check extends StatelessWidget {
  const _Check({required this.selected});
  final bool selected;
  @override
  Widget build(BuildContext context) {
    return Container(
      width: 24,
      height: 24,
      decoration: BoxDecoration(
        color: selected ? AppColors.primary : Colors.transparent,
        borderRadius: BorderRadius.circular(7),
        border: Border.all(color: selected ? AppColors.primary : AppColors.border, width: 2),
      ),
      alignment: Alignment.center,
      child: selected ? const Icon(Icons.check, size: 16, color: Colors.white) : null,
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.label, required this.color});
  final String label;
  final Color color;
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
      ),
      child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700)),
    );
  }
}

class _RequestBadge extends StatelessWidget {
  const _RequestBadge({required this.estado});
  final String? estado;
  @override
  Widget build(BuildContext context) {
    final (label, color) = _map(estado);
    return _Badge(label: label, color: color);
  }

  (String, Color) _map(String? e) {
    switch (e) {
      case 'PENDIENTE_TAXISTA':
        return ('Pendiente', AppColors.warning);
      case 'CONFIRMADO_TAXISTA':
      case 'MONTO_EDITADO_CONFIRMADO':
        return ('Confirmado', AppColors.primary);
      case 'RECHAZADO_TAXISTA':
      case 'CANCELADO':
        return ('Rechazado', AppColors.danger);
      case 'PENDIENTE_BACKOFFICE':
      case 'EN_PROCESO_BACKOFFICE':
        return ('En proceso', AppColors.accent);
      case 'COMPLETADO':
        return ('Completado', AppColors.success);
      case 'OBSERVADO':
        return ('Observado', AppColors.warning);
      default:
        return (e ?? '—', AppColors.muted);
    }
  }
}

/// Barra de accion para generar la factura con las carreras seleccionadas.
class _GenerarBar extends StatelessWidget {
  const _GenerarBar({
    required this.count,
    required this.total,
    required this.submitting,
    required this.onGenerar,
  });
  final int count;
  final double total;
  final bool submitting;
  final VoidCallback onGenerar;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.3)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('$count ${count == 1 ? 'carrera' : 'carreras'}',
                    style: AppText.small.copyWith(color: AppColors.primaryDark)),
                Text(Fmt.money(total), style: AppText.h2.copyWith(color: AppColors.primaryDark)),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          SizedBox(
            height: 48,
            child: ElevatedButton(
              onPressed: submitting ? null : onGenerar,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: AppColors.primary.withValues(alpha: 0.5),
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
              ),
              child: submitting
                  ? const SizedBox(
                      height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white))
                  : const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        AppIcon('receipt-tax', size: 18, color: Colors.white),
                        SizedBox(width: 8),
                        Text('Generar factura',
                            style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white)),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Card informativa (encabezado explicativo de una seccion).
class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.icon, required this.title, required this.text});
  final String icon;
  final String title;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(12)),
            alignment: Alignment.center,
            child: AppIcon(icon, size: 22, color: AppColors.primary),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AppText.bodyStrong.copyWith(color: AppColors.primaryDark)),
                const SizedBox(height: 4),
                Text(text, style: AppText.small.copyWith(color: AppColors.primaryDark)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionLoader extends StatelessWidget {
  const _SectionLoader();
  @override
  Widget build(BuildContext context) => const Center(
        child: Padding(
          padding: EdgeInsets.all(AppSpacing.xl),
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      );
}

class _SectionError extends StatelessWidget {
  const _SectionError();
  @override
  Widget build(BuildContext context) =>
      const EmptyHint(text: 'No pudimos cargar la informacion. Desliza hacia abajo para reintentar.');
}
