import 'package:intl/intl.dart';

/// Formateadores de moneda y fecha (es_PE).
class Fmt {
  Fmt._();

  static final NumberFormat _money = NumberFormat.currency(locale: 'es_PE', symbol: 'S/ ', decimalDigits: 2);
  static final DateFormat _date = DateFormat('dd MMM yyyy', 'es');
  static final DateFormat _month = DateFormat('MMMM yyyy', 'es');

  static String money(num? value) => _money.format(value ?? 0);
  static String date(DateTime? d) => d == null ? '—' : _date.format(d);
  static String month(DateTime d) => _month.format(d);

  /// Periodo tributario AAAA-MM.
  static String period(DateTime d) => DateFormat('yyyy-MM').format(d);
}
