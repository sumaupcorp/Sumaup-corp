import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../application/taxi_providers.dart';
import '../domain/receipt_request.dart';
import 'widgets/corporate_request_card.dart';
import 'widgets/qr_request_card.dart';

/// Pantalla completa de historial y gestión de solicitudes con filtros y búsqueda.
/// Soporta de forma estricta ambos orígenes:
///  - 'QR': Solicitudes directas de pasajeros escaneando el QR.
///  - 'CORPORATIVO': Solicitudes de facturación a la empresa / central.
class SolicitudesListScreen extends ConsumerStatefulWidget {
  const SolicitudesListScreen({
    super.key,
    required this.origen,
    required this.title,
    this.empresa,
  });

  /// 'QR' o 'CORPORATIVO'
  final String origen;
  final String title;
  final String? empresa;

  @override
  ConsumerState<SolicitudesListScreen> createState() => _SolicitudesListScreenState();
}

class _SolicitudesListScreenState extends ConsumerState<SolicitudesListScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';
  String _selectedStatus = 'TODOS';

  static const _qrFilters = [
    (id: 'TODOS', label: 'Todas'),
    (id: 'PENDIENTE', label: 'Por confirmar'),
    (id: 'CONFIRMADO', label: 'Confirmadas'),
    (id: 'EN_PROCESO', label: 'En proceso'),
    (id: 'COMPLETADO', label: 'Emitidas'),
    (id: 'OBSERVADO', label: 'Observadas'),
    (id: 'RECHAZADO', label: 'Rechazadas'),
  ];

  static const _corpFilters = [
    (id: 'TODOS', label: 'Todas'),
    (id: 'PENDIENTE_BACKOFFICE', label: 'En revisión'),
    (id: 'EN_PROCESO_BACKOFFICE', label: 'En emisión'),
    (id: 'COMPLETADO', label: 'Emitidas'),
    (id: 'OBSERVADO', label: 'Observadas'),
    (id: 'CANCELADO', label: 'Canceladas'),
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  bool get _isQr => widget.origen.toUpperCase() == 'QR';

  List<({String id, String label})> get _filters => _isQr ? _qrFilters : _corpFilters;

  bool _matchesStatus(ReceiptRequestModel r, String filter) {
    if (filter == 'TODOS') return true;

    if (_isQr) {
      switch (filter) {
        case 'PENDIENTE':
          return r.isPendiente || r.estado == 'PENDIENTE_TAXISTA';
        case 'CONFIRMADO':
          return r.estado == 'CONFIRMADO_TAXISTA' || r.estado == 'MONTO_EDITADO_CONFIRMADO';
        case 'EN_PROCESO':
          return r.estado == 'PENDIENTE_BACKOFFICE' || r.estado == 'EN_PROCESO_BACKOFFICE';
        case 'COMPLETADO':
          return r.estado == 'COMPLETADO';
        case 'OBSERVADO':
          return r.estado == 'OBSERVADO';
        case 'RECHAZADO':
          return r.estado == 'RECHAZADO_TAXISTA' || r.estado == 'CANCELADO';
        default:
          return r.estado == filter;
      }
    } else {
      return r.estado == filter;
    }
  }

  bool _matchesSearch(ReceiptRequestModel r, String query) {
    if (query.isEmpty) return true;
    final q = query.toLowerCase();
    final name = (r.customerName ?? '').toLowerCase();
    final doc = (r.docNumber ?? '').toLowerCase();
    final tipo = (r.tipo ?? '').toLowerCase();
    final obs = (r.observacion ?? '').toLowerCase();
    final motivo = (r.motivo ?? '').toLowerCase();
    final montoStr = r.montoFinal.toString();
    final montoFmt = Fmt.money(r.montoFinal).toLowerCase();

    return name.contains(q) ||
        doc.contains(q) ||
        tipo.contains(q) ||
        obs.contains(q) ||
        motivo.contains(q) ||
        montoStr.contains(q) ||
        montoFmt.contains(q);
  }

  int _countForFilter(List<ReceiptRequestModel> list, String filterId) {
    return list.where((r) => _matchesStatus(r, filterId)).length;
  }

  void _clearFilters() {
    setState(() {
      _searchController.clear();
      _searchQuery = '';
      _selectedStatus = 'TODOS';
    });
  }

  @override
  Widget build(BuildContext context) {
    final asyncData = _isQr
        ? ref.watch(qrRequestsProvider)
        : ref.watch(corporateRequestsProvider);

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(
        title: Text(widget.title),
        elevation: 0,
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.ink,
      ),
      body: SafeArea(
        child: asyncData.when(
          loading: () => const Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          ),
          error: (error, _) => Center(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.xl),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: AppColors.danger),
                  const SizedBox(height: AppSpacing.md),
                  const Text('Error al cargar las solicitudes', style: AppText.title),
                  const SizedBox(height: AppSpacing.xs),
                  const Text(
                    'No se pudo obtener la información. Por favor, reintenta.',
                    style: AppText.small,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  ElevatedButton(
                    onPressed: () {
                      if (_isQr) {
                        ref.invalidate(qrRequestsProvider);
                        ref.invalidate(taxiRequestsProvider);
                      } else {
                        ref.invalidate(corporateRequestsProvider);
                      }
                    },
                    child: const Text('Reintentar'),
                  ),
                ],
              ),
            ),
          ),
          data: (rawList) {
            final allList = rawList
                .where((r) => _isQr ? r.isQr : r.isCorporativo)
                .toList();

            final filteredList = allList.where((r) {
              if (!_matchesStatus(r, _selectedStatus)) return false;
              return _matchesSearch(r, _searchQuery);
            }).toList();

            final totalMonto = filteredList.fold<double>(0.0, (s, r) => s + r.montoFinal);

            return RefreshIndicator(
              color: AppColors.primary,
              onRefresh: () async {
                if (_isQr) {
                  ref.invalidate(qrRequestsProvider);
                  ref.invalidate(taxiRequestsProvider);
                } else {
                  ref.invalidate(corporateRequestsProvider);
                }
              },
              child: Column(
                children: [
                  // Buscador y Barra de Filtros en contenedor superior
                  Container(
                    color: AppColors.surface,
                    padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.md),
                    child: Column(
                      children: [
                        // Search bar
                        TextField(
                          controller: _searchController,
                          onChanged: (val) => setState(() => _searchQuery = val.trim()),
                          decoration: InputDecoration(
                            hintText: _isQr
                                ? 'Buscar por pasajero, DNI o monto...'
                                : 'Buscar por empresa, documento o monto...',
                            hintStyle: AppText.small.copyWith(color: AppColors.muted),
                            prefixIcon: const Padding(
                              padding: EdgeInsets.symmetric(horizontal: 12),
                              child: AppIcon('search', size: 18, color: AppColors.muted),
                            ),
                            prefixIconConstraints: const BoxConstraints(minWidth: 42, minHeight: 40),
                            suffixIcon: _searchQuery.isNotEmpty
                                ? IconButton(
                                    icon: const Icon(Icons.close, size: 18, color: AppColors.muted),
                                    onPressed: () {
                                      _searchController.clear();
                                      setState(() => _searchQuery = '');
                                    },
                                  )
                                : null,
                            filled: true,
                            fillColor: AppColors.surfaceAlt,
                            contentPadding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 10),
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(AppSpacing.radius),
                              borderSide: const BorderSide(color: AppColors.border),
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(AppSpacing.radius),
                              borderSide: const BorderSide(color: AppColors.border),
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(AppSpacing.radius),
                              borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
                            ),
                          ),
                        ),
                        const SizedBox(height: AppSpacing.md),
                        // Chips de estados horizontales
                        SizedBox(
                          height: 36,
                          child: ListView.separated(
                            scrollDirection: Axis.horizontal,
                            itemCount: _filters.length,
                            separatorBuilder: (_, __) => const SizedBox(width: 8),
                            itemBuilder: (context, index) {
                              final f = _filters[index];
                              final isSelected = _selectedStatus == f.id;
                              final count = _countForFilter(allList, f.id);

                              return InkWell(
                                onTap: () => setState(() => _selectedStatus = f.id),
                                borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 180),
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                  decoration: BoxDecoration(
                                    color: isSelected ? AppColors.primary : AppColors.surfaceAlt,
                                    borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                                    border: Border.all(
                                      color: isSelected ? AppColors.primary : AppColors.border,
                                      width: 1,
                                    ),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                        f.label,
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                                          color: isSelected ? Colors.white : AppColors.body,
                                        ),
                                      ),
                                      if (count > 0) ...[
                                        const SizedBox(width: 6),
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                                          decoration: BoxDecoration(
                                            color: isSelected
                                                ? Colors.white.withValues(alpha: 0.25)
                                                : AppColors.primary.withValues(alpha: 0.12),
                                            borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                                          ),
                                          child: Text(
                                            '$count',
                                            style: TextStyle(
                                              fontSize: 10,
                                              fontWeight: FontWeight.w700,
                                              color: isSelected ? Colors.white : AppColors.primary,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Barra resumen de conteo y montos
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
                    color: AppColors.surfaceAlt,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '${filteredList.length} ${filteredList.length == 1 ? 'solicitud' : 'solicitudes'}',
                          style: AppText.small.copyWith(fontWeight: FontWeight.w600),
                        ),
                        if (filteredList.isNotEmpty)
                          Text(
                            'Total: ${Fmt.money(totalMonto)}',
                            style: AppText.bodyStrong.copyWith(color: AppColors.primaryDark),
                          ),
                      ],
                    ),
                  ),

                  // Lista de resultados
                  Expanded(
                    child: filteredList.isEmpty
                        ? _buildEmptyState(hasFilters: _searchQuery.isNotEmpty || _selectedStatus != 'TODOS')
                        : ListView.builder(
                            padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.xl),
                            itemCount: filteredList.length,
                            itemBuilder: (context, index) {
                              final req = filteredList[index];
                              if (_isQr) {
                                return QrRequestCard(request: req);
                              } else {
                                return CorporateRequestCard(
                                  request: req,
                                  empresa: widget.empresa ?? 'tu central',
                                );
                              }
                            },
                          ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildEmptyState({required bool hasFilters}) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.xxl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(32),
              ),
              alignment: Alignment.center,
              child: Icon(
                hasFilters ? Icons.search_off_rounded : Icons.receipt_long_outlined,
                size: 32,
                color: AppColors.primary,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              hasFilters ? 'No encontramos coincidencias' : 'Aún no hay solicitudes',
              style: AppText.title,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              hasFilters
                  ? 'Prueba modificando el texto de búsqueda o cambiando el filtro de estado.'
                  : _isQr
                      ? 'Las solicitudes de clientes que escaneen tu QR aparecerán en esta lista.'
                      : 'Las solicitudes de facturación que envíes a tu empresa aparecerán aquí.',
              style: AppText.small,
              textAlign: TextAlign.center,
            ),
            if (hasFilters) ...[
              const SizedBox(height: AppSpacing.lg),
              OutlinedButton.icon(
                onPressed: _clearFilters,
                icon: const Icon(Icons.refresh, size: 16),
                label: const Text('Limpiar filtros'),
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
