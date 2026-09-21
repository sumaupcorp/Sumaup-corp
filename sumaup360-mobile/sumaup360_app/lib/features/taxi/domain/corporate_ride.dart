/// Carrera corporativa del taxista (cara taxista).
///
/// Son las carreras que el taxista hace para una central (ej. Taxi Satelital 355)
/// donde el pasajero paga con tarjeta dentro de la app de la central: el dinero
/// va directo a la empresa, y el taxista debe facturarle a la empresa para cobrar.
class CorporateRide {
  const CorporateRide({
    required this.id,
    this.fecha,
    this.origen,
    this.destino,
    required this.monto,
    this.moneda = 'PEN',
    this.medioPago,
    this.estado,
    this.empresa,
    this.invoiceId,
  });

  final String id;
  final DateTime? fecha;
  final String? origen;
  final String? destino;
  final double monto;
  final String moneda;
  final String? medioPago; // TARJETA | APP
  final String? estado; // PENDIENTE_FACTURA | FACTURADA | PAGADA | RECHAZADA
  final String? empresa;
  final String? invoiceId;

  /// Solo se pueden seleccionar/facturar las carreras aun no facturadas.
  bool get facturable => estado == 'PENDIENTE_FACTURA' || estado == null;

  bool get pagada => estado == 'PAGADA';

  /// Etiqueta de ruta legible para la tarjeta.
  String get rutaLabel {
    final o = (origen ?? '').trim();
    final d = (destino ?? '').trim();
    if (o.isEmpty && d.isEmpty) return 'Carrera';
    if (o.isEmpty) return d;
    if (d.isEmpty) return o;
    return '$o  →  $d';
  }

  factory CorporateRide.fromJson(Map<String, dynamic> j) => CorporateRide(
        id: j['id'] as String,
        fecha: j['fecha'] != null ? DateTime.tryParse(j['fecha'] as String) : null,
        origen: j['origen'] as String?,
        destino: j['destino'] as String?,
        monto: (j['monto'] as num?)?.toDouble() ?? 0,
        moneda: (j['moneda'] as String?) ?? 'PEN',
        medioPago: j['medioPago'] as String?,
        estado: j['estado'] as String?,
        empresa: j['empresa'] as String?,
        invoiceId: j['invoiceId'] as String?,
      );
}
