import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/router.dart';
import '../../profile/domain/profile_me.dart';
import '../data/orientation_repository.dart';
import '../domain/orientation_questions.dart';

/// Paso del onboarding donde retomar segun lo que el usuario ya guardo en el
/// backend. Permite que al reinstalar la app o entrar desde otro telefono con
/// el mismo correo continue donde se quedo (los pasos opcionales que omitio
/// no dejan rastro, asi que vuelve al opcional mas cercano).
String registrationResumeRoute(ProfileMe p) {
  if ((p.clientType ?? '').isNotEmpty) return Routes.registerRucOption;
  if ((p.phone ?? '').isNotEmpty) return Routes.registerNotifications;
  if ((p.firstName ?? '').isNotEmpty) return Routes.registerPhoto;
  return Routes.registerName;
}

/// Borrador del flujo de registro: lo que el usuario va eligiendo entre
/// pantallas. La persistencia real ocurre paso a paso contra el backend
/// (profileRepository / fiscalRepository); esto solo evita refetch entre pasos.
class RegistrationDraft {
  const RegistrationDraft({
    this.activity,
    this.answers = const <OrientationAnswer?>[null, null, null, null, null],
    this.result,
  });

  /// TAXISTA | DELIVERY_PEYA | SERVICIOS_PROFESIONALES (registro-5).
  final String? activity;

  /// Respuestas del cuestionario de orientacion (5 posiciones).
  final List<OrientationAnswer?> answers;

  /// Resultado del analisis IA (para la pantalla final).
  final OrientationResult? result;

  List<OrientationAnswer> get completedAnswers => answers.whereType<OrientationAnswer>().toList();

  RegistrationDraft copyWith({
    String? activity,
    List<OrientationAnswer?>? answers,
    OrientationResult? result,
  }) {
    return RegistrationDraft(
      activity: activity ?? this.activity,
      answers: answers ?? this.answers,
      result: result ?? this.result,
    );
  }
}

class RegistrationController extends Notifier<RegistrationDraft> {
  @override
  RegistrationDraft build() => const RegistrationDraft();

  void setActivity(String activity) {
    // Cambiar de actividad invalida las respuestas previas del cuestionario.
    if (activity != state.activity) {
      state = RegistrationDraft(activity: activity);
    }
  }

  void setAnswer(int index, OrientationAnswer answer) {
    final answers = [...state.answers];
    answers[index] = answer;
    state = state.copyWith(answers: answers);
  }

  void setResult(OrientationResult result) {
    state = state.copyWith(result: result);
  }
}

final registrationControllerProvider =
    NotifierProvider<RegistrationController, RegistrationDraft>(RegistrationController.new);
