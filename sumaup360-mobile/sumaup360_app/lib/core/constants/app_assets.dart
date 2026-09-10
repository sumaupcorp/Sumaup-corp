/// Mapa central de assets de la mascota Suma. Nombres limpios -> rutas reales.
/// Las imagenes originales viven en sumaup360-reborn; aqui solo se referencian las copiadas.
class AppAssets {
  AppAssets._();

  static const String _suma = 'assets/suma';

  // Bienvenida / saludo
  static const String sumaBienvenida = '$_suma/suma_bienvenida.png'; // login landing
  static const String sumaSaludo = '$_suma/suma_saludo.png';         // home greeting

  // Onboarding
  static const String sumaTablet = '$_suma/suma_tablet.png';         // "Registra tus comprobantes"
  static const String sumaAnalizando = '$_suma/suma_analizando.png'; // "Deja que Suma te ayude"
  static const String sumaCalendario = '$_suma/suma_calendario.png'; // "Llega preparado al cierre"

  // Diagnostico
  static const String sumaPensando = '$_suma/suma_pensando.png';       // intro / procesando
  static const String sumaRecomendando = '$_suma/suma_recomendando.png'; // resultado / recomendacion

  // Estados
  static const String sumaCelebrando = '$_suma/suma_celebrando.png';   // exito
  static const String sumaConfirmando = '$_suma/suma_confirmando.png'; // verificacion / like
  static const String sumaError = '$_suma/suma_error.png';             // error_view
  static const String sumaEsperando = '$_suma/suma_esperando.png';     // vacio / loading
  static const String sumaCalma = '$_suma/suma_calma.png';

  // Alertas
  static const String sumaAlertaSuave = '$_suma/suma_alerta_suave.png';
  static const String sumaAlertaUrgente = '$_suma/suma_alerta_urgente.png';

  // Soporte / chatbot
  static const String sumaSoporte = '$_suma/suma_soporte.png';

  // Splash (PNG extraido del SVG de Figma: el SVG original envolvia un raster
  // en <pattern>, que flutter_svg no renderiza).
  static const String splashLogo = 'assets/images/splash_sumaup.png';

  // Onboarding (arte de Figma)
  static const String onboarding1 = 'assets/images/onboarding/onboarding_1.png';
  static const String onboarding2 = 'assets/images/onboarding/onboarding_2.png';
  static const String onboarding3 = 'assets/images/onboarding/onboarding_3.png';

  // Inicio / auth landing (logo PNG extraido del SVG de Figma, igual que el splash)
  static const String logoLogin = 'assets/images/logo_login.png';
  static const String icGoogle = 'assets/icons/ic_google.svg';
  static const String icApple = 'assets/icons/ic_apple.svg';
  static const String arrowChevron = 'assets/icons/arrow_chevron.svg';
  static const String icEye = 'assets/icons/ic_eye.svg';
  static const String icEyeSlash = 'assets/icons/ic_eye_slash.svg';

  // Recuperar contrasena (iconos 3D de los mockups)
  static const String icCandado = 'assets/icons/ic_candado.png';
  static const String icEmailConfirmacion = 'assets/icons/ic_email_confirmacion.png';

  // Onboarding de registro (iconos 3D de los mockups)
  static const String icPersona = 'assets/icons/ic_persona.png';
  static const String icPersonInput = 'assets/icons/ic_person_input.png'; // icono del campo nombre
  static const String icCampana = 'assets/icons/ic_campana.png';
  static const String icCarro = 'assets/icons/ic_carro.png';
  static const String icMoto = 'assets/icons/ic_moto.png';
  static const String icProfesional = 'assets/icons/ic_profesional.png';
  static const String icSunat = 'assets/icons/ic_sunat.png';
  static const String icNoRuc = 'assets/icons/ic_no_ruc.png';
  static const String icCheckSeccion = 'assets/icons/ic_check_seccion.png';
  static const String icRayo = 'assets/icons/ic_rayo.png';
  static const String icCohete = 'assets/icons/ic_cohete.png';

  // Iconos
  static const String arrowRight = 'assets/icons/arrow_right.svg';
}
