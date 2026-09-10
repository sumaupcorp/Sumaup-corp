import 'dart:io';

import 'package:dio/dio.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';

import '../../../core/errors/app_exception.dart';
import '../domain/peya_upload.dart';

class PeyaRepository {
  PeyaRepository(this._dio);
  final Dio _dio;

  /// Sube el PDF de ventas a Firebase Storage y devuelve la URL de descarga.
  Future<String> uploadPdf(File file) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) throw AppException('Sesion no valida.');
    final name = 'ventas_${DateTime.now().microsecondsSinceEpoch}.pdf';
    final ref = FirebaseStorage.instance.ref('peya/$uid/$name');
    await ref.putFile(file);
    return ref.getDownloadURL();
  }

  Future<PeyaUpload> create({required String periodo, required String pdfUrl, String? observacion}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/peya/uploads', data: {
        'periodo': periodo,
        'pdfUrl': pdfUrl,
        'observacion': observacion,
      });
      return PeyaUpload.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<PeyaUpload>> uploads() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/peya/uploads');
      return (res.data ?? []).map((e) => PeyaUpload.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<PeyaFile>> files(String uploadId) async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/peya/uploads/$uploadId/files');
      return (res.data ?? []).map((e) => PeyaFile.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
