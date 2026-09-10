import 'package:flutter_test/flutter_test.dart';
import 'package:sumaup360_app/core/utils/validators.dart';

void main() {
  test('email validator', () {
    expect(Validators.email(''), isNotNull);
    expect(Validators.email('malo'), isNotNull);
    expect(Validators.email('a@b.com'), isNull);
  });

  test('password validator', () {
    expect(Validators.password('123'), isNotNull);
    expect(Validators.password('123456'), isNull);
  });
}
