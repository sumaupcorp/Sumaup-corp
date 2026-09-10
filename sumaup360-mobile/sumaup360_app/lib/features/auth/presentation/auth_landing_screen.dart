import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../application/auth_providers.dart';

/// Pantalla de inicio (despues del onboarding): ondas azules pintadas en
/// codigo, logo centrado, CTA de registro y accesos con Google/Apple.
/// Diseno basado en assets/mockups/screen/inicio-screen.png.
class AuthLandingScreen extends ConsumerWidget {
  const AuthLandingScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final size = MediaQuery.sizeOf(context);

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SizedBox(
              // Proporcion medida del mockup: la onda baja 0.67 del ancho.
              height: math.min(size.width * 0.67, size.height * 0.35),
              child: const CustomPaint(painter: _WavesPainter()),
            ),
            const Spacer(flex: 2),
            Center(
              child: Image.asset(AppAssets.logoLogin, width: size.width * 0.72),
            ),
            const Spacer(flex: 2),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xl + AppSpacing.lg),
              child: _CreateAccountButton(onPressed: () => context.push(Routes.register)),
            ),
            const SizedBox(height: AppSpacing.xl),
            Center(
              child: TextButton(
                onPressed: () => context.push(Routes.login),
                style: TextButton.styleFrom(foregroundColor: AppColors.onboardingButton),
                child: const Text(
                  '¿Ya tienes cuenta?',
                  style: TextStyle(
                    fontFamily: 'Poppins',
                    fontSize: 17,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ),
            const Spacer(flex: 2),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _SocialCircle(
                  asset: AppAssets.icGoogle,
                  onTap: () async {
                    await ref.read(authControllerProvider).signInWithGoogle();
                    if (context.mounted) context.go(Routes.home);
                  },
                ),
                const SizedBox(width: 72),
                _SocialCircle(
                  asset: AppAssets.icApple,
                  onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Apple estara disponible pronto.')),
                  ),
                ),
              ],
            ),
            SizedBox(height: AppSpacing.xxxl + AppSpacing.xl + MediaQuery.paddingOf(context).bottom),
          ],
        ),
      ),
    );
  }
}

/// Cabecera: onda azul principal + banda celeste en la esquina superior
/// derecha, dibujadas con curvas Bezier (no requiere SVG).
class _WavesPainter extends CustomPainter {
  const _WavesPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    canvas.clipRect(Offset.zero & size);

    // Onda azul principal (geometria medida del mockup inicio-screen.png):
    // el borde inferior baja a toda la altura a la izquierda y sube en S
    // hasta media altura a la derecha.
    final dark = Paint()..color = AppColors.loginWave;
    final darkPath = Path()
      ..moveTo(0, 0)
      ..lineTo(0, h)
      ..cubicTo(w * 0.35, h * 1.02, w * 0.75, h * 0.52, w, h * 0.50)
      ..lineTo(w, 0)
      ..close();
    canvas.drawPath(darkPath, dark);

    // Circulo celeste sobre la esquina superior derecha (centro fuera del
    // lienzo, arriba a la derecha), tangente a la onda azul.
    final light = Paint()..color = AppColors.loginWaveLight;
    canvas.drawCircle(Offset(w * 0.838, -w * 0.114), w * 0.466, light);
  }

  @override
  bool shouldRepaint(covariant _WavesPainter oldDelegate) => false;
}

/// Boton principal: fill 272727, radio 10, texto Poppins 17 + chevron blanco.
class _CreateAccountButton extends StatelessWidget {
  const _CreateAccountButton({required this.onPressed});
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.onboardingButton,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: onPressed,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 19),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'Crea una cuenta',
                style: TextStyle(
                  fontFamily: 'Poppins',
                  fontSize: 17,
                  fontWeight: FontWeight.w500,
                  color: AppColors.onboardingButtonText,
                ),
              ),
              const SizedBox(width: 12),
              SvgPicture.asset(AppAssets.arrowChevron, height: 14),
            ],
          ),
        ),
      ),
    );
  }
}

/// Boton circular blanco con sombra suave para login social.
class _SocialCircle extends StatelessWidget {
  const _SocialCircle({required this.asset, required this.onTap});
  final String asset;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 68,
      height: 68,
      decoration: const BoxDecoration(
        color: Colors.white,
        shape: BoxShape.circle,
        boxShadow: AppShadows.floating,
      ),
      child: Material(
        color: Colors.transparent,
        shape: const CircleBorder(),
        child: InkWell(
          onTap: onTap,
          customBorder: const CircleBorder(),
          child: Center(child: SvgPicture.asset(asset, width: 28, height: 28)),
        ),
      ),
    );
  }
}
