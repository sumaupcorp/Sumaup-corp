/// Comprobante subido por el usuario (mapea ReceiptResponse del backend).
class Receipt {
  Receipt({
    required this.id,
    required this.type,
    this.docNumber,
    this.issueDate,
    this.amount,
    required this.currency,
    this.fileUrl,
    required this.status,
    this.notes,
  });

  final String id;
  final String type;
  final String? docNumber;
  final DateTime? issueDate;
  final double? amount;
  final String currency;
  final String? fileUrl;
  final String status;
  final String? notes;

  factory Receipt.fromJson(Map<String, dynamic> j) => Receipt(
        id: j['id'] as String,
        type: (j['type'] as String?) ?? 'OTHER',
        docNumber: j['docNumber'] as String?,
        issueDate: j['issueDate'] != null ? DateTime.parse(j['issueDate'] as String) : null,
        amount: (j['amount'] as num?)?.toDouble(),
        currency: (j['currency'] as String?) ?? 'PEN',
        fileUrl: j['fileUrl'] as String?,
        status: (j['status'] as String?) ?? 'PENDING',
        notes: j['notes'] as String?,
      );
}

/// Tipos de documento (para la UI; el backend los normaliza).
const receiptDocTypes = <String>['Boleta', 'Factura', 'Ticket', 'Otro'];

/// Etiqueta amigable del estado.
String receiptStatusLabel(String s) {
  switch (s) {
    case 'PENDING':
      return 'Pendiente';
    case 'IN_PROCESS':
      return 'En proceso';
    case 'PROCESSED':
      return 'Procesado';
    case 'OBSERVED':
      return 'Observado';
    default:
      return s;
  }
}
