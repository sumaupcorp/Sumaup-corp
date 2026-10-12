import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/formatters.dart';
import '../../../../shared/widgets/app_icon.dart';
import '../../domain/receipt_request.dart';
import '../receipt_detail_screen.dart';
import '../solicitar_factura_screen.dart';

/// Tarjeta de solicitud corporativa ingresada directamente por el taxista para cobrar a la empresa.
class CorporateRequestCard extends StatelessWidget {
  const CorporateRequestCard({
    super.key,
    required this.request,
    required this.empresa,
  });

  final ReceiptRequestModel request;
  final String empresa;

  @override
  Widget build(BuildContext context) {
    final r = request;
    final canEdit = r.canTaxistaEdit;

    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
        border: Border.all(
          color: AppColors.primary.withValues(alpha: 0.15),
          width: 1,
        ),
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
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: AppColors.primarySoft,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      alignment: Alignment.center,
                      child: const AppIcon('receipt-tax', size: 20, color: AppColors.primary),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: AppColors.primary.withValues(alpha: 0.1),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  'CORPORATIVO',
                                  style: AppText.small.copyWith(
                                    color: AppColors.primary,
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
                                    letterSpacing: 0.5,
                                  ),
                                ),
                              ),
                              const Spacer(),
                              CorporateStatusBadge(estado: r.estado),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text('Monto solicitado', style: AppText.small.copyWith(color: AppColors.muted)),
                          Text(Fmt.money(r.montoFinal), style: AppText.title.copyWith(color: AppColors.primaryDark)),
                        ],
                      ),
                    ),
                  ],
                ),
                Row(
                  children: [
                    const Spacer(),
                    if (canEdit)
                      OutlinedButton.icon(
                        icon: const AppIcon('edit', size: 14, color: AppColors.primary),
                        label: const Text(
                          'Editar',
                          style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.primary),
                        ),
                        style: OutlinedButton.styleFrom(
                          side: BorderSide(color: AppColors.primary.withValues(alpha: 0.3)),
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radiusPill)),
                          visualDensity: VisualDensity.compact,
                        ),
                        onPressed: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => SolicitarFacturaScreen(
                              empresa: empresa,
                              initialRequest: r,
                            ),
                          ),
                        ),
                      )
                    else
                      const Row(
                        children: [
                          Text('Ver detalle', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                          SizedBox(width: 4),
                          Icon(Icons.chevron_right, size: 16, color: AppColors.muted),
                        ],
                      ),
                  ],
                ),
                if (r.estado == 'OBSERVADO' && r.motivo != null && r.motivo!.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.sm),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.warning.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      'Motivo: ${r.motivo}',
                      style: const TextStyle(fontSize: 11, color: AppColors.warning, fontWeight: FontWeight.w500),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class CorporateStatusBadge extends StatelessWidget {
  const CorporateStatusBadge({super.key, required this.estado});
  final String? estado;

  @override
  Widget build(BuildContext context) {
    final (label, color) = _map(estado);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
      ),
      child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w700)),
    );
  }

  (String, Color) _map(String? e) {
    switch (e) {
      case 'PENDIENTE_BACKOFFICE':
        return ('En revision', AppColors.primary);
      case 'EN_PROCESO_BACKOFFICE':
        return ('En emision', AppColors.accent);
      case 'COMPLETADO':
        return ('Emitida', AppColors.success);
      case 'OBSERVADO':
        return ('Observada', AppColors.warning);
      case 'CANCELADO':
        return ('Cancelada', AppColors.danger);
      default:
        return (e ?? '—', AppColors.muted);
    }
  }
}
