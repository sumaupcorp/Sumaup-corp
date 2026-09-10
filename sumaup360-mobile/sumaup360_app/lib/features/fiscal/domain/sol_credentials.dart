/// Estado (enmascarado) de la conexion SUNAT / Clave SOL de la persona.
/// El backend NUNCA devuelve la clave en claro: solo si esta configurada.
class SolCredentials {
  const SolCredentials({this.docMode, this.ruc, this.solUser, this.dni, this.hasPassword = false});

  final String? docMode; // 'dni' | 'ruc'
  final String? ruc;
  final String? solUser;
  final String? dni;
  final bool hasPassword;

  bool get isDni => docMode == 'dni';
  bool get isConnected => hasPassword;

  factory SolCredentials.fromJson(Map<String, dynamic> j) => SolCredentials(
        docMode: j['docMode'] as String?,
        ruc: j['ruc'] as String?,
        solUser: j['solUser'] as String?,
        dni: j['dni'] as String?,
        hasPassword: j['hasPassword'] as bool? ?? false,
      );
}

/// Resultado de validar la Clave SOL (login real en SUNAT).
class SolValidation {
  const SolValidation({required this.ok, this.nombre, this.detail});

  final bool ok;
  final String? nombre;
  final String? detail;

  factory SolValidation.fromJson(Map<String, dynamic> j) => SolValidation(
        ok: j['ok'] as bool? ?? false,
        nombre: j['nombre'] as String?,
        detail: j['detail'] as String?,
      );
}
