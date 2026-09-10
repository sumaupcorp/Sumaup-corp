import 'package:flutter_svg/flutter_svg.dart';
import 'package:flutter_test/flutter_test.dart';

// Los SVG de splash y onboarding deben parsear con flutter_svg
// (el logo del splash lleva un bitmap embebido via pattern).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('el logo del splash parsea sin errores', () async {
    final info = await vg.loadPicture(const SvgAssetLoader('assets/images/splash_sumaup.svg'), null);
    expect(info.size.width, greaterThan(0));
    info.picture.dispose();
  });

  test('la flecha de los botones parsea sin errores', () async {
    final info = await vg.loadPicture(const SvgAssetLoader('assets/icons/arrow_right.svg'), null);
    expect(info.size.width, greaterThan(0));
    info.picture.dispose();
  });
}
