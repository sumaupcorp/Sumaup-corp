// Catalogo de planes por segmento (Linea Personas).
//
// La idea de producto: el plan NO es un menu de opciones; se RECOMIENDA segun
// el diagnostico/segmento del cliente. Un taxista ve su Plan App Taxi, un
// repartidor su Plan App Delivery, recibos por honorarios su Plan Honorarios, etc.

class PlanBenefit {
  const PlanBenefit(this.icon, this.title);
  final String icon; // nombre para AppIcon
  final String title;
}

class SegmentPlan {
  const SegmentPlan({
    required this.segment,
    required this.code,
    required this.name,
    required this.tagline,
    required this.monthly,
    required this.benefits,
  });

  final String segment; // TAXISTA, DELIVERY, ...
  final String code; // codigo de plan backend (APP, HONORARIO, ...)
  final String name; // nombre comercial
  final String tagline;
  final double monthly; // precio mensual referencial (PEN)
  final List<PlanBenefit> benefits;
}

/// Nombre amigable del segmento.
String segmentLabel(String? code) {
  switch (code) {
    case 'HONORARIOS':
      return 'Recibos por honorarios';
    case 'TAXISTA':
      return 'Taxista';
    case 'DELIVERY':
      return 'Delivery / apps';
    case 'NEGOCIO_RUS':
      return 'Negocio (Nuevo RUS)';
    case 'ALQUILERES':
      return 'Alquileres';
    default:
      return 'Tu actividad';
  }
}

const _validacionBenefit = PlanBenefit('history-toggle', 'Opcion de revision por un contador SUMAUP');

/// Plan recomendado para cada segmento. null si el segmento no esta mapeado
/// (ej. NO_SE) -> la UI invita a completar el diagnostico.
const Map<String, SegmentPlan> _bySegment = {
  'HONORARIOS': SegmentPlan(
    segment: 'HONORARIOS',
    code: 'HONORARIO',
    name: 'Plan Honorarios',
    tagline: 'Para recibos por honorarios (4ta categoria)',
    monthly: 12.90,
    benefits: [
      PlanBenefit('receipt-tax', 'Registra tus recibos por honorarios'),
      PlanBenefit('circle-dashed-percentage', 'Calcula tu retencion (8%) y renta'),
      PlanBenefit('bell-ringing', 'Alertas de tus pagos a cuenta'),
      PlanBenefit('brand-google-analytics', 'Reporte mensual de tus ingresos'),
      _validacionBenefit,
    ],
  ),
  'TAXISTA': SegmentPlan(
    segment: 'TAXISTA',
    code: 'APP',
    name: 'Plan App Taxi',
    tagline: 'Hecho para taxistas formales',
    monthly: 14.90,
    benefits: [
      PlanBenefit('receipt-tax', 'Registra tus viajes e ingresos diarios'),
      PlanBenefit('pig-money', 'Control de combustible y gastos'),
      PlanBenefit('circle-dashed-percentage', 'Tu regimen (RUS o renta) al dia'),
      PlanBenefit('bell-ringing', 'Alertas de cuotas y vencimientos'),
      _validacionBenefit,
    ],
  ),
  'DELIVERY': SegmentPlan(
    segment: 'DELIVERY',
    code: 'APP',
    name: 'Plan App Delivery',
    tagline: 'Para repartidores y trabajadores de apps',
    monthly: 14.90,
    benefits: [
      PlanBenefit('receipt-tax', 'Ingresos por app y propinas'),
      PlanBenefit('pig-money', 'Gastos de moto, combustible y mantenimiento'),
      PlanBenefit('circle-dashed-percentage', 'Calculo de impuestos segun tu regimen'),
      PlanBenefit('bell-ringing', 'Alertas de tus obligaciones'),
      _validacionBenefit,
    ],
  ),
  'NEGOCIO_RUS': SegmentPlan(
    segment: 'NEGOCIO_RUS',
    code: 'BASICO',
    name: 'Plan Negocio',
    tagline: 'Para bodegas y minimarkets (Nuevo RUS)',
    monthly: 19.90,
    benefits: [
      PlanBenefit('receipt-tax', 'Registra tus ventas diarias'),
      PlanBenefit('circle-dashed-percentage', 'Tu categoria y cuota del Nuevo RUS'),
      PlanBenefit('moneybag', 'Control de ingresos y gastos'),
      PlanBenefit('bell-ringing', 'Alertas de tu cuota mensual'),
      _validacionBenefit,
    ],
  ),
  'ALQUILERES': SegmentPlan(
    segment: 'ALQUILERES',
    code: 'BASICO',
    name: 'Plan Alquileres',
    tagline: 'Para arrendadores de propiedades',
    monthly: 16.90,
    benefits: [
      PlanBenefit('receipt-tax', 'Registra tus rentas (1ra categoria)'),
      PlanBenefit('circle-dashed-percentage', 'Tu impuesto de 5% calculado'),
      PlanBenefit('folder-share', 'Genera tus recibos de arrendamiento'),
      PlanBenefit('bell-ringing', 'Alertas de tus pagos mensuales'),
      _validacionBenefit,
    ],
  ),
};

/// Devuelve el plan recomendado para el segmento, o null si no esta mapeado.
SegmentPlan? planForSegment(String? segment) => segment == null ? null : _bySegment[segment];
