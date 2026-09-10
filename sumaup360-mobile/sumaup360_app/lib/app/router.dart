import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../features/splash/presentation/splash_screen.dart';
import '../features/onboarding/presentation/onboarding_screen.dart';
import '../features/auth/presentation/auth_landing_screen.dart';
import '../features/auth/presentation/login_screen.dart';
import '../features/auth/presentation/register_screen.dart';
import '../features/auth/presentation/forgot_password_screen.dart';
import '../features/auth/presentation/check_email_screen.dart';
import '../features/auth/presentation/complete_profile_screen.dart';
import '../features/fiscal/domain/ruc_fiscal.dart';
import '../features/registration/presentation/activate_account_screen.dart';
import '../features/registration/presentation/register_name_screen.dart';
import '../features/registration/presentation/register_photo_screen.dart';
import '../features/registration/presentation/register_phone_screen.dart';
import '../features/registration/presentation/register_notifications_screen.dart';
import '../features/registration/presentation/register_activity_screen.dart';
import '../features/registration/presentation/register_ruc_option_screen.dart';
import '../features/registration/presentation/register_ruc_screen.dart';
import '../features/registration/presentation/register_ruc_confirm_screen.dart';
import '../features/registration/presentation/register_done_screen.dart';
import '../features/registration/presentation/orientation_intro_screen.dart';
import '../features/registration/presentation/orientation_quiz_screen.dart';
import '../features/registration/presentation/orientation_analysis_screen.dart';
import '../features/registration/presentation/orientation_result_screen.dart';
import '../features/home/presentation/home_screen.dart';
import '../features/fiscal/presentation/sunat_connect_screen.dart';
import '../features/plans/presentation/plan_screen.dart';
import '../features/profile/presentation/account_screen.dart';
import '../features/chatbot/presentation/chat_screen.dart';
import '../features/aidiagnosis/presentation/ai_diagnosis_screen.dart';
import '../features/income/presentation/income_form_screen.dart';
import '../features/expenses/presentation/expense_form_screen.dart';
import '../features/receipts/presentation/receipt_upload_screen.dart';
import '../features/stats/presentation/stats_screen.dart';

/// Rutas de la app.
class Routes {
  static const splash = '/splash';
  static const onboarding = '/onboarding';
  static const auth = '/auth';
  static const login = '/login';
  static const register = '/register';
  static const forgot = '/forgot';
  static const forgotSent = '/forgot/sent';
  static const completeProfile = '/complete-profile';

  // Flujo de registro / onboarding de cuenta
  static const registerActivate = '/register/activate';
  static const registerName = '/register/name';
  static const registerPhoto = '/register/photo';
  static const registerPhone = '/register/phone';
  static const registerNotifications = '/register/notifications';
  static const registerActivity = '/register/activity';
  static const registerRucOption = '/register/ruc-option';
  static const registerRuc = '/register/ruc';
  static const registerRucConfirm = '/register/ruc-confirm';
  static const registerDone = '/register/done';
  static const orientationIntro = '/register/orientation';
  static const orientationQuiz = '/register/orientation/quiz';
  static const orientationAnalysis = '/register/orientation/analysis';
  static const orientationResult = '/register/orientation/result';
  static const home = '/home';

  static const incomeForm = '/income/new';
  static const expenseForm = '/expense/new';
  static const receiptUpload = '/receipts/upload';
  static const stats = '/stats';
  static const plans = '/plans';
  static const sunat = '/sunat';
  static const account = '/account';
  static const chat = '/chat';
  static const aiDiagnosis = '/ai-diagnosis';
}

final appRouter = GoRouter(
  initialLocation: Routes.splash,
  routes: [
    GoRoute(path: Routes.splash, builder: (_, __) => const SplashScreen()),
    GoRoute(path: Routes.onboarding, builder: (_, __) => const OnboardingScreen()),
    GoRoute(path: Routes.auth, builder: (_, __) => const AuthLandingScreen()),
    GoRoute(path: Routes.login, builder: (_, __) => const LoginScreen()),
    GoRoute(path: Routes.register, builder: (_, __) => const RegisterScreen()),
    GoRoute(path: Routes.forgot, builder: (_, __) => const ForgotPasswordScreen()),
    GoRoute(
      path: Routes.forgotSent,
      builder: (_, state) => CheckEmailScreen(email: state.extra as String? ?? ''),
    ),
    GoRoute(path: Routes.completeProfile, builder: (_, __) => const CompleteProfileScreen()),
    GoRoute(path: Routes.registerActivate, builder: (_, __) => const ActivateAccountScreen()),
    GoRoute(path: Routes.registerName, builder: (_, __) => const RegisterNameScreen()),
    GoRoute(path: Routes.registerPhoto, builder: (_, __) => const RegisterPhotoScreen()),
    GoRoute(path: Routes.registerPhone, builder: (_, __) => const RegisterPhoneScreen()),
    GoRoute(path: Routes.registerNotifications, builder: (_, __) => const RegisterNotificationsScreen()),
    GoRoute(path: Routes.registerActivity, builder: (_, __) => const RegisterActivityScreen()),
    GoRoute(path: Routes.registerRucOption, builder: (_, __) => const RegisterRucOptionScreen()),
    GoRoute(path: Routes.registerRuc, builder: (_, __) => const RegisterRucScreen()),
    GoRoute(
      path: Routes.registerRucConfirm,
      builder: (_, state) => RegisterRucConfirmScreen(fiscal: state.extra as RucFiscal),
    ),
    GoRoute(path: Routes.registerDone, builder: (_, __) => const RegisterDoneScreen()),
    GoRoute(path: Routes.orientationIntro, builder: (_, __) => const OrientationIntroScreen()),
    GoRoute(path: Routes.orientationQuiz, builder: (_, __) => const OrientationQuizScreen()),
    GoRoute(path: Routes.orientationAnalysis, builder: (_, __) => const OrientationAnalysisScreen()),
    GoRoute(path: Routes.orientationResult, builder: (_, __) => const OrientationResultScreen()),
    GoRoute(path: Routes.home, builder: (_, __) => const HomeScreen()),
    GoRoute(path: Routes.incomeForm, builder: (_, __) => const IncomeFormScreen()),
    GoRoute(path: Routes.expenseForm, builder: (_, __) => const ExpenseFormScreen()),
    GoRoute(path: Routes.receiptUpload, builder: (_, __) => const ReceiptUploadScreen()),
    GoRoute(path: Routes.stats, builder: (_, __) => const StatsScreen()),
    GoRoute(path: Routes.plans, builder: (_, __) => const PlanScreen()),
    GoRoute(path: Routes.sunat, builder: (_, __) => const SunatConnectScreen()),
    GoRoute(path: Routes.account, builder: (_, __) => const AccountScreen()),
    GoRoute(path: Routes.chat, builder: (_, __) => const ChatScreen()),
    GoRoute(path: Routes.aiDiagnosis, builder: (_, __) => const AiDiagnosisScreen()),
  ],
  errorBuilder: (_, state) => Scaffold(
    body: Center(child: Text('Ruta no encontrada: ${state.uri}')),
  ),
);
