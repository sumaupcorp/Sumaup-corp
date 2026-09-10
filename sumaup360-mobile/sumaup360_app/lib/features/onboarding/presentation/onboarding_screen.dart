import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/storage/secure_storage_service.dart';

class _Slide {
  const _Slide(this.image, this.title, this.text, this.cta);
  final String image;
  final String title;
  final String text;
  final String cta;
}

const _slides = <_Slide>[
  _Slide(
    AppAssets.onboarding1,
    'Empieza con el control\nde tu actividad',
    'Organiza tus comprobantes, revisa tus movimientos y mantén tu información en un solo lugar.',
    'Siguiente',
  ),
  _Slide(
    AppAssets.onboarding2,
    'Todo más claro y\nordenado',
    'Consulta tus ingresos, gastos y comprobantes de forma simple para entender mejor tu actividad.',
    'Siguiente',
  ),
  _Slide(
    AppAssets.onboarding3,
    'Todo listo para\nempezar',
    'Mantén tus ingresos, gastos y comprobantes organizados en un solo lugar y toma el control de tu actividad desde el primer día.',
    'Comenzar',
  ),
];

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _controller = PageController();
  int _index = 0;

  bool get _isLast => _index == _slides.length - 1;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _finish() async {
    await SecureStorageService().setOnboardingSeen();
    if (mounted) context.go(Routes.auth);
  }

  void _next() {
    if (_isLast) {
      _finish();
    } else {
      _controller.nextPage(duration: const Duration(milliseconds: 380), curve: Curves.easeOutCubic);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _controller,
                onPageChanged: (i) => setState(() => _index = i),
                itemCount: _slides.length,
                itemBuilder: (_, i) => _SlideView(slide: _slides[i]),
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(AppSpacing.xl, 0, AppSpacing.xl, AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _Dots(count: _slides.length, index: _index),
                    const SizedBox(height: AppSpacing.xxl + AppSpacing.sm),
                    Row(
                      children: [
                        AnimatedOpacity(
                          duration: const Duration(milliseconds: 200),
                          opacity: _isLast ? 0 : 1,
                          child: TextButton(
                            onPressed: _isLast ? null : _finish,
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: Size.zero,
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              foregroundColor: AppColors.body,
                            ),
                            child: const Text(
                              'Saltar',
                              style: TextStyle(
                                fontFamily: 'Poppins',
                                fontSize: 15,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ),
                        const Spacer(),
                        _NextButton(label: _slides[_index].cta, onPressed: _next),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Arte arriba a sangre completa (entra bajo la barra de estado) + texto abajo.
class _SlideView extends StatelessWidget {
  const _SlideView({required this.slide});
  final _Slide slide;

  @override
  Widget build(BuildContext context) {
    // Tipografia relativa al ancho (base 390): crece en pantallas grandes
    // y se reduce en chicas sin romper el layout.
    final scale = (MediaQuery.sizeOf(context).width / 390).clamp(0.85, 1.3);
    final titleSize = 30.0 * scale;
    // Texto descriptivo: Poppins Regular 14, con ancho limitado para que
    // quiebre en 3 lineas como en el mockup (no de borde a borde).
    final bodySize = 14.0 * scale;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Flexible (loose): la imagen usa su alto natural y el texto queda
        // pegado justo debajo; en pantallas bajas se recorta el pie del arte
        // en vez de desbordar.
        Flexible(
          child: SafeArea(
            bottom: false,
            child: Image.asset(
              slide.image,
              width: double.infinity,
              fit: BoxFit.fitWidth,
              alignment: Alignment.topCenter,
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        Padding(
          padding: const EdgeInsets.fromLTRB(
              AppSpacing.xl, 0, AppSpacing.xl, AppSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                slide.title,
                textAlign: TextAlign.left,
                style: TextStyle(
                  fontFamily: 'Poppins',
                  fontSize: titleSize,
                  fontWeight: FontWeight.w700,
                  color: AppColors.onboardingTitle,
                  height: 1.25,
                  letterSpacing: titleSize * -0.02,
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              Padding(
                padding: const EdgeInsets.only(right: AppSpacing.xxxl),
                child: Text(
                  slide.text,
                  textAlign: TextAlign.left,
                  style: TextStyle(
                    fontFamily: 'Poppins',
                    fontSize: bodySize,
                    fontWeight: FontWeight.w400,
                    color: AppColors.body,
                    height: 1.5,
                    letterSpacing: bodySize * -0.02,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Boton oscuro (fill 272727, radio 10) con la flecha arrow_right.svg.
class _NextButton extends StatelessWidget {
  const _NextButton({required this.label, required this.onPressed});
  final String label;
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
          padding: const EdgeInsets.symmetric(horizontal: 26, vertical: 16),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 200),
                child: Text(
                  label,
                  key: ValueKey(label),
                  style: const TextStyle(
                    fontFamily: 'Poppins',
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppColors.onboardingButtonText,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              SvgPicture.asset(AppAssets.arrowRight, width: 18),
            ],
          ),
        ),
      ),
    );
  }
}

class _Dots extends StatelessWidget {
  const _Dots({required this.count, required this.index});
  final int count;
  final int index;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: List.generate(count, (i) {
        final active = i == index;
        return AnimatedContainer(
          duration: const Duration(milliseconds: 320),
          curve: Curves.easeOutCubic,
          margin: const EdgeInsets.only(right: 8),
          height: active ? 10 : 6,
          width: active ? 46 : 18,
          decoration: BoxDecoration(
            color: active ? AppColors.onboardingButton : AppColors.dotInactive,
            borderRadius: BorderRadius.circular(99),
          ),
        );
      }),
    );
  }
}
