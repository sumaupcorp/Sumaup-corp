import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';

/// Navegacion del flujo de registro: retrocede con pop si hay historial;
/// si la pantalla fue destino de un go() (flujo reanudado tras volver a
/// iniciar sesion) no hay nada que hacer pop y se navega al paso anterior.
extension RegisterNav on BuildContext {
  void backOr(String previousRoute) {
    if (canPop()) {
      pop();
    } else {
      go(previousRoute);
    }
  }
}

/// Widgets del flujo de registro/onboarding (mockups registro-*.png):
/// cabecera azul con ola, tarjetas de opcion con check, progreso y opciones
/// del cuestionario, botones secundarios. Todo en Poppins.

/// Estilos de texto del flujo.
class RegisterText {
  RegisterText._();

  static const TextStyle title = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 22,
    fontWeight: FontWeight.w700,
    color: AppColors.onboardingTitle,
    letterSpacing: 22 * -0.02,
    height: 1.3,
  );

  static const TextStyle titleCentered = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 24,
    fontWeight: FontWeight.w700,
    color: AppColors.onboardingTitle,
    letterSpacing: 24 * -0.02,
    height: 1.3,
  );

  static const TextStyle body = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: AppColors.authSubtitle,
    height: 1.5,
  );

  static const TextStyle small = TextStyle(
    fontFamily: 'Poppins',
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: AppColors.authHint,
    height: 1.5,
  );
}

/// Cabecera azul con degradado y ola blanca inferior (mockups del registro).
class RegisterHeader extends StatelessWidget {
  const RegisterHeader({super.key, this.onBack});

  /// Si es null no se muestra la flecha de regreso.
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    return SizedBox(
      height: w * 0.36,
      width: double.infinity,
      child: Stack(
        children: [
          const Positioned.fill(child: CustomPaint(painter: _WaveHeaderPainter())),
          if (onBack != null)
            SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.only(left: 10, top: 2),
                child: IconButton(
                  onPressed: onBack,
                  icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 22),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _WaveHeaderPainter extends CustomPainter {
  const _WaveHeaderPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    canvas.clipRect(Offset.zero & size);
    // Fondo azul con degradado vertical (claro arriba, oscuro abajo).
    final rect = Offset.zero & size;
    canvas.drawRect(
      rect,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: AppColors.registerHeaderGradient,
        ).createShader(rect),
    );
    // Ola blanca inferior que abre el area de contenido.
    final wave = Path()
      ..moveTo(0, h * 0.82)
      ..cubicTo(w * 0.28, h * 1.06, w * 0.60, h * 0.50, w, h * 0.78)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    canvas.drawPath(wave, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant _WaveHeaderPainter oldDelegate) => false;
}

/// Tarjeta de opcion con icono 3D, titulo, descripcion y check verde al
/// seleccionar (registro-5.png / registro-6.png).
class RegisterOptionCard extends StatelessWidget {
  const RegisterOptionCard({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.selected,
    required this.onTap,
  });

  final String icon; // asset PNG
  final String title;
  final String subtitle;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
        decoration: BoxDecoration(
          color: selected ? Colors.white : AppColors.optionCardBg,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: selected ? AppColors.onboardingButton : Colors.transparent,
            width: 1.4,
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Image.asset(icon, width: 44, height: 44),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontFamily: 'Poppins',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontFamily: 'Poppins',
                      fontSize: 12.5,
                      fontWeight: FontWeight.w400,
                      color: AppColors.authSubtitle,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
            if (selected) ...[
              const SizedBox(width: 10),
              Image.asset('assets/icons/ic_check_seccion.png', width: 28, height: 28),
            ],
          ],
        ),
      ),
    );
  }
}

/// Barra de progreso segmentada del cuestionario (5 segmentos, el activo azul).
class QuizProgressBar extends StatelessWidget {
  const QuizProgressBar({super.key, required this.current, required this.total});

  final int current; // indice 0-based de la pregunta activa
  final int total;

  @override
  Widget build(BuildContext context) {
    // El segmento activo es claramente mas ancho y alto que el resto, para
    // que se note de un vistazo en que pregunta esta el usuario.
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        for (var i = 0; i < total; i++) ...[
          if (i > 0) const SizedBox(width: 10),
          Expanded(
            flex: i == current ? 5 : 2,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 250),
              curve: Curves.easeOut,
              height: i == current ? 9 : 6,
              decoration: BoxDecoration(
                color: i == current ? AppColors.loginWave : AppColors.quizSegment,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

/// Opcion tipo radio del cuestionario (registro-sub-1-x-x.png).
class QuizOptionTile extends StatelessWidget {
  const QuizOptionTile({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected ? AppColors.loginWave : AppColors.authFieldBorder,
            width: selected ? 1.6 : 1,
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Poppins',
                  fontSize: 14.5,
                  fontWeight: FontWeight.w500,
                  color: AppColors.ink,
                  height: 1.35,
                ),
              ),
            ),
            const SizedBox(width: 10),
            _Radio(selected: selected),
          ],
        ),
      ),
    );
  }
}

class _Radio extends StatelessWidget {
  const _Radio({required this.selected});
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 120),
      width: 22,
      height: 22,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: selected ? AppColors.loginWave : AppColors.authHint,
          width: selected ? 2 : 1.6,
        ),
      ),
      alignment: Alignment.center,
      child: selected
          ? Container(
              width: 11,
              height: 11,
              decoration: const BoxDecoration(color: AppColors.loginWave, shape: BoxShape.circle),
            )
          : null,
    );
  }
}

/// Boton secundario con borde (blanco, borde oscuro, radio 10).
class RegisterOutlineButton extends StatelessWidget {
  const RegisterOutlineButton({super.key, required this.label, required this.onPressed, this.loading = false});
  final String label;
  final VoidCallback onPressed;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
        side: const BorderSide(color: AppColors.onboardingButton, width: 1.2),
      ),
      child: InkWell(
        onTap: loading ? null : onPressed,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Center(
            child: loading
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.onboardingButton),
                  )
                : Text(
                    label,
                    style: const TextStyle(
                      fontFamily: 'Poppins',
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                      color: AppColors.ink,
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}

/// Boton "Continuar" deshabilitado (gris) mientras faltan datos, como en los mockups.
class RegisterDisabledButton extends StatelessWidget {
  const RegisterDisabledButton({super.key, required this.label});
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 17),
      decoration: BoxDecoration(
        color: const Color(0xFFBFC4C9),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Center(
        child: Text(
          label,
          style: const TextStyle(
            fontFamily: 'Poppins',
            fontSize: 18,
            fontWeight: FontWeight.w500,
            color: Colors.white,
          ),
        ),
      ),
    );
  }
}

/// Boton de texto plano ("Omitir", "Ahora no").
class RegisterTextButton extends StatelessWidget {
  const RegisterTextButton({super.key, required this.label, required this.onPressed});
  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: GestureDetector(
        onTap: onPressed,
        behavior: HitTestBehavior.opaque,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 15.5,
              fontWeight: FontWeight.w500,
              color: AppColors.ink,
            ),
          ),
        ),
      ),
    );
  }
}

/// Campo de solo lectura con label (registro-sub-3.png).
class RegisterReadOnlyField extends StatelessWidget {
  const RegisterReadOnlyField({super.key, required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontFamily: 'Poppins',
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: AppColors.onboardingTitle,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
          decoration: BoxDecoration(
            color: AppColors.surfaceAlt,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.authFieldBorder),
          ),
          child: Text(
            value.isEmpty ? '-' : value,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 14.5,
              fontWeight: FontWeight.w400,
              color: AppColors.ink,
            ),
          ),
        ),
      ],
    );
  }
}
