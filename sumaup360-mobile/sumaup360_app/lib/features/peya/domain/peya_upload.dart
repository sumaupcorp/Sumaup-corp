// Carga mensual de Peya (cara repartidor) + archivos resultado.

class PeyaUpload {
  const PeyaUpload({
    required this.id,
    this.periodo,
    this.pdfUrl,
    this.estado,
    this.observacionUsuario,
    this.observacionBackoffice,
    this.codigoNps,
    this.createdAt,
    this.completedAt,
  });

  final String id;
  final String? periodo;
  final String? pdfUrl;
  final String? estado;
  final String? observacionUsuario;
  final String? observacionBackoffice;
  final String? codigoNps;
  final String? createdAt;
  final String? completedAt;

  factory PeyaUpload.fromJson(Map<String, dynamic> j) => PeyaUpload(
        id: j['id'] as String,
        periodo: j['periodo'] as String?,
        pdfUrl: j['pdfUrl'] as String?,
        estado: j['estado'] as String?,
        observacionUsuario: j['observacionUsuario'] as String?,
        observacionBackoffice: j['observacionBackoffice'] as String?,
        codigoNps: j['codigoNps'] as String?,
        createdAt: j['createdAt'] as String?,
        completedAt: j['completedAt'] as String?,
      );
}

class PeyaFile {
  const PeyaFile({required this.id, required this.fileUrl, this.fileName});
  final String id;
  final String fileUrl;
  final String? fileName;

  factory PeyaFile.fromJson(Map<String, dynamic> j) => PeyaFile(
        id: j['id'] as String,
        fileUrl: (j['fileUrl'] as String?) ?? '',
        fileName: j['fileName'] as String?,
      );
}
