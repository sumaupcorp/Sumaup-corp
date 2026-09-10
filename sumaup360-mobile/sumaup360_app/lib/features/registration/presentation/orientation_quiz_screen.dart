import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_spacing.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import '../../profile/application/profile_providers.dart';
import '../application/registration_controller.dart';
import '../domain/orientation_questions.dart';
import 'widgets/register_decor.dart';

/// Cuestionario de orientacion: 5 preguntas segun la actividad elegida
/// (mockups registro-sub-1-1-x / 1-2-x / 1-3-x).
class OrientationQuizScreen extends ConsumerStatefulWidget {
  const OrientationQuizScreen({super.key});

  @override
  ConsumerState<OrientationQuizScreen> createState() => _OrientationQuizScreenState();
}

class _OrientationQuizScreenState extends ConsumerState<OrientationQuizScreen> {
  int _index = 0;
  final Map<int, int> _selected = {}; // pregunta -> opcion elegida

  void _back() {
    if (_index == 0) {
      context.backOr(Routes.orientationIntro);
    } else {
      setState(() => _index--);
    }
  }

  void _next(List<QuizQuestion> questions) {
    final choice = _selected[_index];
    if (choice == null) return;
    final question = questions[_index];
    ref.read(registrationControllerProvider.notifier).setAnswer(
          _index,
          OrientationAnswer(question: question.text, answer: question.options[choice]),
        );
    if (_index < questions.length - 1) {
      setState(() => _index++);
    } else {
      context.push(Routes.orientationAnalysis);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Si se entra desde el home (orientacion pendiente) el borrador no tiene
    // actividad: usar la del perfil guardado.
    final activity = ref.watch(registrationControllerProvider).activity ??
        ref.watch(profileMeProvider).valueOrNull?.clientType;
    final questions = questionsForActivity(activity);
    final question = questions[_index];
    final hasChoice = _selected[_index] != null;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SafeArea(
          top: false,
          child: Column(
            children: [
              RegisterHeader(onBack: _back),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, 0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text('Pregunta ${_index + 1} de ${questions.length}', style: RegisterText.title),
                      const SizedBox(height: AppSpacing.lg),
                      QuizProgressBar(current: _index, total: questions.length),
                      const SizedBox(height: AppSpacing.xl),
                      Text(
                        question.text,
                        style: const TextStyle(
                          fontFamily: 'Poppins',
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                          color: AppColorsQuiz.question,
                          height: 1.4,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xl),
                      for (var i = 0; i < question.options.length; i++) ...[
                        QuizOptionTile(
                          label: question.options[i],
                          selected: _selected[_index] == i,
                          onTap: () => setState(() => _selected[_index] = i),
                        ),
                        const SizedBox(height: AppSpacing.md + 2),
                      ],
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: hasChoice
                    ? AuthPrimaryButton(label: 'Continuar', onPressed: () => _next(questions))
                    : const RegisterDisabledButton(label: 'Continuar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Color del enunciado de la pregunta (mas oscuro que el titulo de seccion).
class AppColorsQuiz {
  AppColorsQuiz._();
  static const Color question = Color(0xFF37474F);
}
