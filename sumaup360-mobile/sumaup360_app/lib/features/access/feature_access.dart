import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers.dart';

/// Resultado del chequeo de acceso a una feature (bloqueo premium).
class FeatureAccess {
  const FeatureAccess({required this.allowed, this.reason, this.planRequired, this.price});
  final bool allowed;
  final String? reason; // REQUIRES_PREMIUM | ALLOWED
  final String? planRequired; // TAXISTA_PREMIUM | PEYA_PREMIUM
  final double? price;

  factory FeatureAccess.fromJson(Map<String, dynamic> j) => FeatureAccess(
        allowed: (j['allowed'] as bool?) ?? false,
        reason: j['reason'] as String?,
        planRequired: j['planRequired'] as String?,
        price: (j['price'] as num?)?.toDouble(),
      );
}

class FeatureRepository {
  FeatureRepository(this._dio);
  final Dio _dio;

  Future<FeatureAccess> access(String feature) async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/features/$feature/access');
      return FeatureAccess.fromJson(res.data ?? {});
    } on DioException catch (_) {
      // Ante error, bloquear por defecto (no abrir premium por fallo de red).
      return const FeatureAccess(allowed: false, reason: 'ERROR');
    }
  }
}

final featureRepositoryProvider = Provider<FeatureRepository>((ref) => FeatureRepository(ref.watch(dioProvider)));

/// Acceso a una feature concreta (ej. TAXI_COMPROBANTES).
final featureAccessProvider = FutureProvider.family<FeatureAccess, String>((ref, feature) {
  return ref.watch(featureRepositoryProvider).access(feature);
});
