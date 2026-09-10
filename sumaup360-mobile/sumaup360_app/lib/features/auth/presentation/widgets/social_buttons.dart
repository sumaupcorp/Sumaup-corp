import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../../../shared/widgets/secondary_button.dart';

/// Botones de login social. Google operativo; Apple preparado (placeholder).
class SocialButtons extends StatelessWidget {
  const SocialButtons({super.key, this.onGoogle});
  final Future<void> Function()? onGoogle;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SecondaryButton(
          label: 'Continuar con Google',
          icon: SvgPicture.asset('assets/icons/Google.svg', width: 22, height: 22),
          onPressed: onGoogle == null ? null : () => onGoogle!(),
        ),
        const SizedBox(height: 12),
        SecondaryButton(
          label: 'Continuar con Apple',
          icon: SvgPicture.asset('assets/icons/Apple.svg', width: 22, height: 22),
          onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Apple estara disponible pronto.')),
          ),
        ),
      ],
    );
  }
}
