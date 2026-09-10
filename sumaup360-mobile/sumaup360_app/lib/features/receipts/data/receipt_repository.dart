import 'dart:io';

import 'package:dio/dio.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';

import '../../../core/errors/app_exception.dart';
import '../domain/receipt.dart';

class ReceiptRepository {
  ReceiptRepository(this._dio);
  final Dio _dio;

  Future<List<Receipt>> list() async {
    try {
      final res = await _dio.get<List<dynamic>>('/app/receipts');
      return (res.data ?? []).map((e) => Receipt.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Sube la imagen a Firebase Storage y devuelve la ruta del objeto (file_path).
  /// El bucket es privado; el backoffice lo lee con signed URL.
  Future<String> uploadFile(File file) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) throw AppException('Sesion no valida.');
    final ext = file.path.contains('.') ? file.path.split('.').last : 'jpg';
    final name = '${DateTime.now().microsecondsSinceEpoch}.$ext';
    final ref = FirebaseStorage.instance.ref('receipts/$uid/$name');
    await ref.putFile(file);
    return ref.fullPath;
  }

  Future<void> create({
    String? docNumber,
    DateTime? issueDate,
    double? amount,
    String? fileUrl,
    String? notes,
  }) async {
    try {
      await _dio.post<Map<String, dynamic>>('/app/receipts', data: {
        'docNumber': docNumber,
        'issueDate': issueDate?.toIso8601String().substring(0, 10),
        'amount': amount,
        'currency': 'PEN',
        'fileUrl': fileUrl,
        'notes': notes,
      });
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
