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
import 'solicitar_factura_screen.dart';
import 'solicitudes_list_screen.dart';
import 'widgets/corporate_request_card.dart';

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

  void _mostrarNuevaSolicitud(String empresa) {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: AppColors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => _NuevaSolicitudSheet(empresa: empresa),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ridesAsync = ref.watch(corporateRidesProvider);
    final rides = ridesAsync.valueOrNull ?? const <CorporateRide>[];
    final facturables = rides.where((r) => r.facturable).toList();
    final historial = rides.where((r) => !r.facturable).toList();

    final corporateRequests = ref.watch(corporateRequestsProvider).valueOrNull ?? const [];
    final corpPendientes = corporateRequests.where((r) => r.estado != 'COMPLETADO' && r.estado != 'CANCELADO').toList();
    final corpCompletadas = corporateRequests.where((r) => r.estado == 'COMPLETADO').toList();

    final porFacturar = facturables.fold<double>(0, (s, r) => s + r.monto) +
        corpPendientes.fold<double>(0, (s, r) => s + r.montoFinal);
    final countFacturar = facturables.length + corpPendientes.length;
    final facturado = historial.fold<double>(0, (s, r) => s + r.monto) +
        corpCompletadas.fold<double>(0, (s, r) => s + r.montoFinal);

    final empresa = rides
            .map((r) => r.empresa)
            .firstWhere((e) => (e ?? '').trim().isNotEmpty, orElse: () => null) ??
        corporateRequests
            .map((r) => r.customerName)
            .firstWhere((e) => (e ?? '').trim().isNotEmpty, orElse: () => null) ??
        'tu central';

    final body = RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(corporateRidesProvider);
        ref.invalidate(corporateRequestsProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          if (!widget.embedded) ...[const Text('Corporativo', style: AppText.h1), const SizedBox(height: AppSpacing.lg)],
          _HeroCard(
            porFacturar: porFacturar,
            countFacturar: countFacturar,
            facturado: facturado,
            empresa: empresa,
          ),
          const SizedBox(height: AppSpacing.lg),
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
            onNuevaSolicitud: () => _mostrarNuevaSolicitud(empresa),
          ),
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



/// Segmento Corporativo: carreras por facturar + solicitudes de factura + historial.
class _CorporativoSection extends ConsumerWidget {
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
    this.onNuevaSolicitud,
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
  final VoidCallback? onNuevaSolicitud;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedRides = facturables.where((r) => selected.contains(r.id)).toList();
    final selTotal = selectedRides.fold<double>(0, (s, r) => s + r.monto);
    final allSelected = facturables.isNotEmpty && selected.containsAll(facturables.map((r) => r.id).toSet());

    final requestsAsync = ref.watch(corporateRequestsProvider);
    final corporateRequests = requestsAsync.valueOrNull ?? [];
    final isLoading = (requestsAsync.isLoading && corporateRequests.isEmpty) && ridesAsync.isLoading;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: AppSpacing.lg),
        Row(
          children: [
            const Expanded(child: InfoTitle('Por facturar')),
            _BotonNuevaSolicitud(onTap: onNuevaSolicitud),
          ],
        ),
        if (facturables.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.xs),
          Align(
            alignment: Alignment.centerRight,
            child: GestureDetector(
              onTap: onSelectAll,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Text(
                  allSelected ? 'Quitar todas' : 'Seleccionar todas',
                  style: AppText.small.copyWith(color: AppColors.primary, fontWeight: FontWeight.w600),
                ),
              ),
            ),
          ),
        ],
        const SizedBox(height: AppSpacing.md),
        if (isLoading)
          const _SectionLoader()
        else if (facturables.isEmpty && corporateRequests.isEmpty)
          const EmptyHint(
            text: 'No tienes carreras por facturar en este momento. Toca "+ Nueva solicitud" para ingresar una factura.',
          )
        else
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (facturables.isNotEmpty)
                Column(
                  children: facturables
                      .map((r) => _RideCard(
                            ride: r,
                            selectable: true,
                            selected: selected.contains(r.id),
                            onTap: () => onToggle(r.id),
                          ))
                      .toList(),
                ),
              if (corporateRequests.isNotEmpty) ...[
                if (facturables.isNotEmpty) const SizedBox(height: AppSpacing.lg),
                const SizedBox(height: AppSpacing.sm),
                Column(
                  children: corporateRequests
                      .take(5)
                      .map((r) => CorporateRequestCard(
                            request: r,
                            empresa: empresa,
                          ))
                      .toList(),
                ),
                if (corporateRequests.length > 5) ...[
                  const SizedBox(height: AppSpacing.xs),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton(
                      onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => SolicitudesListScreen(
                            origen: 'CORPORATIVO',
                            title: 'Facturas corporativas',
                            empresa: empresa,
                          ),
                        ),
                      ),
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size.fromHeight(44),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                      ),
                      child: Text('Ver todas las facturas (${corporateRequests.length})'),
                    ),
                  ),
                ],
              ],
            ],
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



/// Boton de accion para crear una nueva solicitud o requerimiento de factura.
class _BotonNuevaSolicitud extends StatelessWidget {
  const _BotonNuevaSolicitud({this.onTap});
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: AppColors.primarySoft,
            borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
            border: Border.all(color: AppColors.primary.withValues(alpha: 0.15)),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppIcon('plus', size: 14, color: AppColors.primary),
              SizedBox(width: 5),
              Text(
                'Nueva solicitud',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Hoja modal que orienta al conductor entre pedir factura o registrar una carrera manual.
class _NuevaSolicitudSheet extends StatelessWidget {
  const _NuevaSolicitudSheet({required this.empresa});
  final String empresa;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: AppColors.primarySoft,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  alignment: Alignment.center,
                  child: const AppIcon('square-plus', size: 20, color: AppColors.primary),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Nueva solicitud', style: AppText.title),
                      const SizedBox(height: 2),
                      Text('Gestiona tus cobros o carreras con $empresa', style: AppText.small),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            _SheetOption(
              icon: 'receipt-tax',
              title: 'Solicitar factura a la empresa',
              subtitle: 'Pide a la central la liquidación o facturación de tus carreras.',
              onTap: () {
                Navigator.pop(context);
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => SolicitarFacturaScreen(empresa: empresa),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _SheetOption extends StatelessWidget {
  const _SheetOption({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final String icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surfaceAlt,
      borderRadius: BorderRadius.circular(AppSpacing.radius),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radius),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(10),
                ),
                alignment: Alignment.center,
                child: AppIcon(icon, size: 20, color: AppColors.primary),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: AppText.bodyStrong),
                    const SizedBox(height: 2),
                    Text(subtitle, style: AppText.small),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, size: 20, color: AppColors.muted),
            ],
          ),
        ),
      ),
    );
  }
}

