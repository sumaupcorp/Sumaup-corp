import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/storage/secure_storage_service.dart';
import '../../profile/application/profile_providers.dart';
import '../../registration/application/registration_controller.dart';

/// Splash: degradado azul de marca + logo centrado que entra desde abajo.
/// Decide a donde ir segun onboarding y sesion.
class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<Offset> _slide;
  late final Animation<double> _fade;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: const Duration(milliseconds: 900));
    _slide = Tween<Offset>(begin: const Offset(0, 0.6), end: Offset.zero)
        .animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic));
    _fade = CurvedAnimation(parent: _controller, curve: Curves.easeOut);
    _controller.forward();
    _decide();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _decide() async {
    // Deja terminar la animacion del logo antes de navegar.
    await Future<void>.delayed(const Duration(milliseconds: 2000));
    if (!mounted) return;
    final seen = await SecureStorageService().isOnboardingSeen();

    User? user;
    try {
      user = FirebaseAuth.instance.currentUser;
    } catch (_) {
      user = null; // Firebase no inicializado en dev
    }
    if (!mounted) return;

    if (!seen) {
      context.go(Routes.onboarding);
    } else if (user == null) {
      context.go(Routes.auth);
    } else if (!user.emailVerified) {
      context.go(Routes.registerActivate);
    } else {
      // Gating: si aun no termino el onboarding de registro, retomarlo en el
      // paso donde se quedo (segun lo ya guardado en el backend).
      // profileCompleted (RUC validado en SUNAT) ya NO bloquea el acceso al Home: conectar
      // SUNAT queda como paso opcional dentro de la app. Fail-open a home si error de red.
      var dest = Routes.home;
      try {
        final p = await ref.read(profileMeProvider.future);
        if (!p.onboardingCompleted) dest = registrationResumeRoute(p);
      } catch (_) {
        // no bloquear por error de red
      }
      if (!mounted) return;
      context.go(dest);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        body: Container(
          width: double.infinity,
          height: double.infinity,
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: AppColors.splashGradient,
              stops: AppColors.splashGradientStops,
            ),
          ),
          child: Center(
            child: FadeTransition(
              opacity: _fade,
              child: SlideTransition(
                position: _slide,
                child: Image.asset(AppAssets.splashLogo, width: 220),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
