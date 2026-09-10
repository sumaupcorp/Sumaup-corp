/// Archivo adjunto a una solicitud (el comprobante que sube el backoffice).
class AttachmentModel {
  const AttachmentModel({
    required this.id,
    required this.fileUrl,
    this.fileName,
    this.createdAt,
  });

  final String id;
  final String fileUrl;
  final String? fileName;
  final String? createdAt;

  factory AttachmentModel.fromJson(Map<String, dynamic> j) => AttachmentModel(
        id: j['id'] as String,
        fileUrl: j['fileUrl'] as String,
        fileName: j['fileName'] as String?,
        createdAt: j['createdAt'] as String?,
      );
}
