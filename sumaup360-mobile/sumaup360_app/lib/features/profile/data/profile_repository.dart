import 'dart:io';

import 'package:dio/dio.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';

import '../../../core/errors/app_exception.dart';
import '../domain/profile_me.dart';

class ProfileRepository {
  ProfileRepository(this._dio);
  final Dio _dio;

  /// Sube la foto de perfil a Firebase Storage (profile/{uid}/avatar...) y
  /// devuelve la URL de descarga, que se guarda como photoUrl en el backend.
  Future<String> uploadAvatar(File file) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) throw AppException('Sesion no valida.');
    final ext = file.path.contains('.') ? file.path.split('.').last : 'jpg';
    final name = 'avatar_${DateTime.now().microsecondsSinceEpoch}.$ext';
    final ref = FirebaseStorage.instance.ref('profile/$uid/$name');
    await ref.putFile(file);
    return ref.getDownloadURL();
  }

  Future<ProfileMe> getMe() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/profile/me');
      return ProfileMe.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<ProfileMe> updateMe({
    String? fullName,
    String? phone,
    String? countryCode,
    String? referralCode,
    String? photoUrl,
    String? clientType,
    String? dni,
    String? firstName,
    String? lastName,
    bool? onboardingCompleted,
    String? orientationStatus, // PENDING | NOT_REQUIRED (COMPLETED lo pone el backend)
  }) async {
    try {
      final res = await _dio.put<Map<String, dynamic>>('/app/profile/me', data: {
        'fullName': fullName,
        'phone': phone,
        'countryCode': countryCode,
        'referralCode': referralCode,
        'photoUrl': photoUrl,
        'clientType': clientType,
        'dni': dni,
        'firstName': firstName,
        'lastName': lastName,
        'onboardingCompleted': onboardingCompleted,
        'orientationStatus': orientationStatus,
      });
      return ProfileMe.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
