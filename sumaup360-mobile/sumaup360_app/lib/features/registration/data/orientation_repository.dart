import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/providers.dart';
import '../domain/orientation_questions.dart';

/// Punto de sustentacion: por que el regimen le conviene, con la regla
/// tributaria concreta (montos, tasas, limites).
class OrientationPoint {
  const OrientationPoint({required this.title, required this.detail});
  final String title;
  final String detail;

  factory OrientationPoint.fromJson(Map<String, dynamic> j) => OrientationPoint(
        title: (j['title'] as String?) ?? '',
        detail: (j['detail'] as String?) ?? '',
      );
}

/// Estimado de lo que pagaria en el regimen recomendado.
class OrientationCost {
  const OrientationCost({required this.label, required this.amount, required this.note});
  final String label; // "Cuota fija mensual"
  final String amount; // "S/ 20.00"
  final String note; // detalle de la categoria/base del calculo

  factory OrientationCost.fromJson(Map<String, dynamic> j) => OrientationCost(
        label: (j['label'] as String?) ?? '',
        amount: (j['amount'] as String?) ?? '',
        note: (j['note'] as String?) ?? '',
      );
}

/// Alternativa si su situacion cambia (plan B).
class OrientationAlternative {
  const OrientationAlternative({required this.regime, required this.when});
  final String regime;
  final String when;

  factory OrientationAlternative.fromJson(Map<String, dynamic> j) => OrientationAlternative(
        regime: (j['regime'] as String?) ?? '',
        when: (j['when'] as String?) ?? '',
      );
}

/// Resultado del analisis de orientacion tributaria (IA en el backend), con
/// sustentacion real: por que conviene, costo estimado, limites y pasos.
class OrientationResult {
  const OrientationResult({
    required this.headline,
    required this.regime,
    required this.summary,
    this.whyItFits = const [],
    this.estimatedCost,
    this.considerations = const [],
    this.nextSteps = const [],
    this.alternative,
    this.disclaimer,
    this.modelo,
  });

  final String headline; // p.ej. "Persona Natural con Negocio"
  final String regime; // p.ej. "Nuevo RUS - Categoría 1"
  final String summary; // frase completa de la recomendacion
  final List<OrientationPoint> whyItFits; // sustentacion real (3-5 puntos)
  final OrientationCost? estimatedCost; // lo que pagaria aprox
  final List<String> considerations; // limites y alertas
  final List<String> nextSteps; // pasos para inscribirse
  final OrientationAlternative? alternative; // plan B si crece
  final String? disclaimer;
  final String? modelo;

  static List<String> _strings(dynamic v) =>
      ((v as List<dynamic>?) ?? []).map((e) => e.toString()).toList();

  factory OrientationResult.fromJson(Map<String, dynamic> j) => OrientationResult(
        headline: (j['headline'] as String?) ?? '',
        regime: (j['regime'] as String?) ?? '',
        summary: (j['summary'] as String?) ?? '',
        whyItFits: ((j['whyItFits'] as List<dynamic>?) ?? [])
            .whereType<Map<String, dynamic>>()
            .map(OrientationPoint.fromJson)
            .toList(),
        estimatedCost: j['estimatedCost'] is Map<String, dynamic>
            ? OrientationCost.fromJson(j['estimatedCost'] as Map<String, dynamic>)
            : null,
        considerations: _strings(j['considerations']),
        nextSteps: _strings(j['nextSteps']),
        alternative: j['alternative'] is Map<String, dynamic>
            ? OrientationAlternative.fromJson(j['alternative'] as Map<String, dynamic>)
            : null,
        disclaimer: j['disclaimer'] as String?,
        modelo: j['modelo'] as String?,
      );
}

/// Estado guardado de la orientacion (para el banner pendiente del home).
class OrientationStatus {
  const OrientationStatus({this.status, this.result});
  final String? status; // PENDING | COMPLETED | NOT_REQUIRED | null
  final OrientationResult? result;

  bool get isPending => status == 'PENDING';

  factory OrientationStatus.fromJson(Map<String, dynamic> j) => OrientationStatus(
        status: j['status'] as String?,
        result: j['result'] is Map<String, dynamic>
            ? OrientationResult.fromJson(j['result'] as Map<String, dynamic>)
            : null,
      );
}

class OrientationRepository {
  OrientationRepository(this._dio);
  final Dio _dio;

  /// Envia las respuestas del cuestionario y devuelve la recomendacion de la IA.
  /// Puede tardar (el backend consulta al modelo).
  Future<OrientationResult> analyze({
    required String activity,
    required List<OrientationAnswer> answers,
  }) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(
        '/app/orientation/analyze',
        data: {
          'activity': activity,
          'answers': answers.map((a) => a.toJson()).toList(),
        },
        options: Options(receiveTimeout: const Duration(seconds: 90)),
      );
      return OrientationResult.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Estado y resultado guardados de la orientacion.
  Future<OrientationStatus> get() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/orientation');
      return OrientationStatus.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}

final orientationRepositoryProvider =
    Provider<OrientationRepository>((ref) => OrientationRepository(ref.watch(dioProvider)));

final orientationStatusProvider =
    FutureProvider<OrientationStatus>((ref) => ref.watch(orientationRepositoryProvider).get());
