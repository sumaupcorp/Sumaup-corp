// Modelos del modulo Servicios Profesionales (Recibos por Honorarios + Suspension de 4ta).

class HonorarioRequestModel {
  const HonorarioRequestModel({
    required this.id,
    this.clienteNombre,
    this.clienteDocType,
    this.clienteDocNumber,
    this.descripcion,
    this.monto,
    this.conRetencion = false,
    this.estado,
    this.reciboUrl,
    this.observacion,
    this.createdAt,
    this.completedAt,
  });

  final String id;
  final String? clienteNombre;
  final String? clienteDocType;
  final String? clienteDocNumber;
  final String? descripcion;
  final double? monto;
  final bool conRetencion;
  final String? estado;
  final String? reciboUrl;
  final String? observacion;
  final String? createdAt;
  final String? completedAt;

  factory HonorarioRequestModel.fromJson(Map<String, dynamic> j) => HonorarioRequestModel(
        id: j['id'] as String,
        clienteNombre: j['clienteNombre'] as String?,
        clienteDocType: j['clienteDocType'] as String?,
        clienteDocNumber: j['clienteDocNumber'] as String?,
        descripcion: j['descripcion'] as String?,
        monto: (j['monto'] as num?)?.toDouble(),
        conRetencion: (j['conRetencion'] as bool?) ?? false,
        estado: j['estado'] as String?,
        reciboUrl: j['reciboUrl'] as String?,
        observacion: j['observacion'] as String?,
        createdAt: j['createdAt'] as String?,
        completedAt: j['completedAt'] as String?,
      );
}

class SuspensionRequestModel {
  const SuspensionRequestModel({
    required this.id,
    this.anio,
    this.estado,
    this.observacion,
    this.constanciaUrl,
    this.createdAt,
    this.completedAt,
  });

  final String id;
  final int? anio;
  final String? estado;
  final String? observacion;
  final String? constanciaUrl;
  final String? createdAt;
  final String? completedAt;

  factory SuspensionRequestModel.fromJson(Map<String, dynamic> j) => SuspensionRequestModel(
        id: j['id'] as String,
        anio: (j['anio'] as num?)?.toInt(),
        estado: j['estado'] as String?,
        observacion: j['observacion'] as String?,
        constanciaUrl: j['constanciaUrl'] as String?,
        createdAt: j['createdAt'] as String?,
        completedAt: j['completedAt'] as String?,
      );
}
