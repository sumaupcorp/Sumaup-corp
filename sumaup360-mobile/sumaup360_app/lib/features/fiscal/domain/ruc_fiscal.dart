/// Datos de la Ficha RUC (SUNAT) guardados en el perfil.
class RucFiscal {
  const RucFiscal({
    this.ruc,
    this.razonSocial,
    this.taxStatus,
    this.taxCondition,
    this.taxpayerType,
    this.economicActivity,
    this.ciiuCode,
    this.fechaInscripcion,
    this.fechaInicioActividades,
    this.domicilioFiscal,
    this.checkedAt,
  });

  final String? ruc;
  final String? razonSocial;      // nombre / razon social
  final String? taxStatus;        // ACTIVO
  final String? taxCondition;     // HABIDO
  final String? taxpayerType;     // PERSONA NATURAL SIN NEGOCIO
  final String? economicActivity; // descripcion CIIU
  final String? ciiuCode;         // 9609
  final String? fechaInscripcion;
  final String? fechaInicioActividades;
  final String? domicilioFiscal;
  final String? checkedAt;

  bool get hasData =>
      ruc != null && ruc!.isNotEmpty ||
      taxStatus != null ||
      taxpayerType != null ||
      economicActivity != null;

  factory RucFiscal.fromJson(Map<String, dynamic> j) => RucFiscal(
        ruc: j['ruc'] as String?,
        razonSocial: j['razonSocial'] as String?,
        taxStatus: j['taxStatus'] as String?,
        taxCondition: j['taxCondition'] as String?,
        taxpayerType: j['taxpayerType'] as String?,
        economicActivity: j['economicActivity'] as String?,
        ciiuCode: j['ciiuCode'] as String?,
        fechaInscripcion: j['fechaInscripcion'] as String?,
        fechaInicioActividades: j['fechaInicioActividades'] as String?,
        domicilioFiscal: j['domicilioFiscal'] as String?,
        checkedAt: j['checkedAt'] as String?,
      );
}
