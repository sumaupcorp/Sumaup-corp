/// Validadores reutilizables para formularios (mensajes en espanol).
class Validators {
  Validators._();

  static final RegExp _email = RegExp(r'^[\w.+-]+@[\w-]+\.[\w.-]+$');

  static String? email(String? v) {
    final value = (v ?? '').trim();
    if (value.isEmpty) return 'Ingresa tu correo';
    if (!_email.hasMatch(value)) return 'Correo invalido';
    return null;
  }

  static String? password(String? v) {
    final value = v ?? '';
    if (value.isEmpty) return 'Ingresa tu contrasena';
    if (value.length < 6) return 'Minimo 6 caracteres';
    return null;
  }

  static String? required(String? v, {String field = 'Este campo'}) {
    if ((v ?? '').trim().isEmpty) return '$field es obligatorio';
    return null;
  }

  static String? confirm(String? v, String other) {
    if (v != other) return 'Las contrasenas no coinciden';
    return null;
  }

  static String? ruc(String? v) {
    final value = (v ?? '').trim();
    if (value.isEmpty) return null; // opcional
    if (value.length != 11 || int.tryParse(value) == null) return 'RUC debe tener 11 digitos';
    return null;
  }
}
