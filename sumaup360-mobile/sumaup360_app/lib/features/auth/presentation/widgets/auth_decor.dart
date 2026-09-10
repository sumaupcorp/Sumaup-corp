import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../../../core/constants/app_colors.dart';

/// Widgets del diseno de register/login (mockups register.png / login.png):
/// cabecera azul con domo blanco, inputs con label, boton principal 272727,
/// botones sociales con borde y enlaces inferiores. Todo en Poppins.

/// Cabecera: fondo azul, circulo celeste en la esquina superior izquierda y
/// domo blanco que abre el area de contenido. Incluye la flecha de regreso.
class AuthHeader extends StatelessWidget {
  const AuthHeader({super.key, this.onBack});
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    return SizedBox(
      height: w * 0.34,
      width: double.infinity,
      child: Stack(
        children: [
          const Positioned.fill(child: CustomPaint(painter: _AuthHeaderPainter())),
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

class _AuthHeaderPainter extends CustomPainter {
  const _AuthHeaderPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    canvas.clipRect(Offset.zero & size);
    // Fondo azul.
    canvas.drawRect(Offset.zero & size, Paint()..color = AppColors.loginWave);
    // Circulo celeste tras la flecha de regreso (esquina superior izquierda).
    canvas.drawCircle(Offset(0, -w * 0.187), w * 0.354, Paint()..color = AppColors.loginWaveLight);
    // Domo blanco: abre el area de contenido (geometria medida del mockup).
    canvas.drawCircle(Offset(w * 0.49, w * 0.933), w * 0.80, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant _AuthHeaderPainter oldDelegate) => false;
}

/// Campo con label arriba (y widget opcional a la derecha del label, como
/// "¿Olvidaste tu contrasena?") y soporte de ojo para contrasenas.
class AuthField extends StatelessWidget {
  const AuthField({
    super.key,
    required this.label,
    required this.controller,
    this.hint,
    this.obscure = false,
    this.onToggleObscure,
    this.validator,
    this.keyboardType,
    this.labelTrailing,
    this.hasError = false,
  });

  final String label;
  final TextEditingController controller;
  final String? hint;
  final bool obscure;

  /// Pinta el borde en rojo (error manejado por la pantalla, ver AuthErrorText).
  final bool hasError;

  /// Si no es null, el campo es de contrasena y muestra el ojo.
  final VoidCallback? onToggleObscure;
  final String? Function(String?)? validator;
  final TextInputType? keyboardType;
  final Widget? labelTrailing;

  OutlineInputBorder _border(Color color, [double width = 1]) => OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: color, width: width),
      );

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
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
            if (labelTrailing != null) ...[const Spacer(), labelTrailing!],
          ],
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          obscureText: obscure,
          validator: validator,
          keyboardType: keyboardType,
          style: const TextStyle(
            fontFamily: 'Poppins',
            fontSize: 14.5,
            color: AppColors.ink,
          ),
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 14,
              fontWeight: FontWeight.w400,
              color: AppColors.authHint,
            ),
            filled: true,
            fillColor: Colors.white,
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
            enabledBorder: _border(hasError ? AppColors.danger : AppColors.authFieldBorder),
            focusedBorder: _border(hasError ? AppColors.danger : AppColors.loginWave, 1.4),
            errorBorder: _border(AppColors.danger),
            focusedErrorBorder: _border(AppColors.danger, 1.4),
            suffixIcon: onToggleObscure == null
                ? null
                : IconButton(
                    onPressed: onToggleObscure,
                    icon: SvgPicture.asset(
                      obscure ? 'assets/icons/ic_eye.svg' : 'assets/icons/ic_eye_slash.svg',
                      width: 22,
                      colorFilter: const ColorFilter.mode(AppColors.authHint, BlendMode.srcIn),
                    ),
                  ),
          ),
        ),
      ],
    );
  }
}

/// Boton principal: fill 272727, radio 10, texto Poppins 18 medium centrado.
class AuthPrimaryButton extends StatelessWidget {
  const AuthPrimaryButton({super.key, required this.label, required this.onPressed, this.loading = false});
  final String label;
  final VoidCallback onPressed;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.onboardingButton,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: loading ? null : onPressed,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 17),
          child: Center(
            child: loading
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Text(
                    label,
                    style: const TextStyle(
                      fontFamily: 'Poppins',
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      color: AppColors.onboardingButtonText,
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}

/// Boton social: blanco con borde suave, icono + texto centrados.
class AuthSocialButton extends StatelessWidget {
  const AuthSocialButton({super.key, required this.icon, required this.label, required this.onPressed});
  final String icon;
  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.authFieldBorder),
      ),
      child: InkWell(
        onTap: onPressed,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 15),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              SvgPicture.asset(icon, width: 22, height: 22),
              const SizedBox(width: 10),
              Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Poppins',
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: AppColors.onboardingTitle,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Separador "O registrate con" / "O inicia sesion con".
class AuthOrDivider extends StatelessWidget {
  const AuthOrDivider({super.key, required this.text});
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Divider(color: AppColors.authFieldBorder)),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: Text(
            text,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 13,
              fontWeight: FontWeight.w400,
              color: AppColors.authHint,
            ),
          ),
        ),
        const Expanded(child: Divider(color: AppColors.authFieldBorder)),
      ],
    );
  }
}

/// Icono 3D centrado dentro de un circulo gris claro (mockups
/// recuperar-contraseña.png / confirmar-email.png).
class AuthIconCircle extends StatelessWidget {
  const AuthIconCircle({super.key, required this.asset});
  final String asset;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 128,
      height: 128,
      decoration: const BoxDecoration(
        color: Color(0xFFF2F3F5),
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Image.asset(asset, width: 72, height: 72),
    );
  }
}

/// Mensaje de error bajo un campo: circulo rojo con signo de exclamacion y
/// texto rojo (mockup error-recuperar-contraseña.png).
class AuthErrorText extends StatelessWidget {
  const AuthErrorText({super.key, required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.only(top: 2),
          child: Icon(Icons.error, color: AppColors.danger, size: 16),
        ),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            message,
            style: const TextStyle(
              fontFamily: 'Poppins',
              fontSize: 12.5,
              fontWeight: FontWeight.w400,
              color: AppColors.danger,
              height: 1.35,
            ),
          ),
        ),
      ],
    );
  }
}

/// Enlace inferior: "¿Ya tienes una cuenta? Inicia sesion".
class AuthBottomLink extends StatelessWidget {
  const AuthBottomLink({super.key, required this.question, required this.action, required this.onTap});
  final String question;
  final String action;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Text.rich(
            TextSpan(
              text: '$question ',
              style: const TextStyle(
                fontFamily: 'Poppins',
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: AppColors.onboardingTitle,
              ),
              children: [
                TextSpan(
                  text: action,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppColors.authLink,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
