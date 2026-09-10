import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:sumaup360_app/core/constants/app_assets.dart';
import 'package:sumaup360_app/core/theme/app_theme.dart';
import 'package:sumaup360_app/features/onboarding/presentation/onboarding_screen.dart';

// Captura del onboarding a 390x844 con Poppins real, para revision visual.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('captura onboarding screen 1', (tester) async {
    for (final f in ['Regular', 'Medium', 'SemiBold', 'Bold']) {
      final loader = FontLoader('Poppins')
        ..addFont(rootBundle.load('assets/fonts/Poppins-$f.ttf'));
      await loader.load();
    }
    await tester.binding.setSurfaceSize(const Size(390, 844));
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;

    await tester.pumpWidget(MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const OnboardingScreen(),
    ));

    // Decodifica las imagenes del arte (en tests son async).
    await tester.runAsync(() async {
      final context = tester.element(find.byType(OnboardingScreen));
      for (final a in [AppAssets.onboarding1, AppAssets.onboarding2, AppAssets.onboarding3]) {
        await precacheImage(AssetImage(a), context);
      }
    });
    for (var i = 0; i < 8; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }

    await expectLater(
      find.byType(OnboardingScreen),
      matchesGoldenFile('goldens/onboarding_1.png'),
    );

    for (final n in [2, 3]) {
      await tester.tap(find.text(n == 2 ? 'Siguiente' : 'Siguiente'));
      await tester.pumpAndSettle();
      await expectLater(
        find.byType(OnboardingScreen),
        matchesGoldenFile('goldens/onboarding_$n.png'),
      );
    }
    stdout.writeln('capturas generadas');
  });
}
