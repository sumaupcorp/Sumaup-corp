/// Solicitud de comprobante (cara taxista).
class ReceiptRequestModel {
  const ReceiptRequestModel({
    required this.id,
    this.tipo,
    this.monto,
    this.montoEditado,
    this.docType,
    this.docNumber,
    this.customerName,
    this.whatsapp,
    this.email,
    this.observacion,
    this.estado,
    this.motivo,
    this.createdAt,
    this.completedAt,
  });

  final String id;
  final String? tipo; // BOLETA | FACTURA
  final double? monto;
  final double? montoEditado;
  final String? docType;
  final String? docNumber;
  final String? customerName;
  final String? whatsapp;
  final String? email;
  final String? observacion;
  final String? estado;
  final String? motivo;
  final String? createdAt;
  final String? completedAt;

  bool get isPendiente => estado == 'PENDIENTE_TAXISTA';
  double get montoFinal => montoEditado ?? monto ?? 0;

  factory ReceiptRequestModel.fromJson(Map<String, dynamic> j) => ReceiptRequestModel(
        id: j['id'] as String,
        tipo: j['tipo'] as String?,
        monto: (j['monto'] as num?)?.toDouble(),
        montoEditado: (j['montoEditado'] as num?)?.toDouble(),
        docType: j['docType'] as String?,
        docNumber: j['docNumber'] as String?,
        customerName: j['customerName'] as String?,
        whatsapp: j['whatsapp'] as String?,
        email: j['email'] as String?,
        observacion: j['observacion'] as String?,
        estado: j['estado'] as String?,
        motivo: j['motivo'] as String?,
        createdAt: j['createdAt'] as String?,
        completedAt: j['completedAt'] as String?,
      );
}
