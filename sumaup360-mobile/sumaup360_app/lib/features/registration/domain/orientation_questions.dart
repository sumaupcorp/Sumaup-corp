/// Cuestionario de orientacion tributaria (5 preguntas por actividad).
/// Textos tomados de los mockups registro-sub-1-x-x.png.
class QuizQuestion {
  const QuizQuestion({required this.text, required this.options});
  final String text;
  final List<String> options;
}

/// Respuesta elegida (se envia al backend para el analisis con IA).
class OrientationAnswer {
  const OrientationAnswer({required this.question, required this.answer});
  final String question;
  final String answer;

  Map<String, dynamic> toJson() => {'question': question, 'answer': answer};
}

const _incomeQuestion = QuizQuestion(
  text: '¿Cuánto estimas ingresar al mes por tu actividad?',
  options: [
    'Hasta S/ 2,000',
    'De S/ 2,001 a S/ 5,000',
    'De S/ 5,001 a S/ 8,000',
    'Más de S/ 8,000',
    'Aún no puedo estimarlo',
  ],
);

const _taxiQuestions = <QuizQuestion>[
  QuizQuestion(
    text: '¿Cómo realizas actualmente tu actividad de taxi?',
    options: [
      'Trabajo por cuenta propia',
      'Trabajo mediante una aplicación',
      'Trabajo para una empresa',
      'Recién voy a comenzar',
    ],
  ),
  QuizQuestion(
    text: '¿A quiénes brindas principalmente el servicio?',
    options: [
      'Personas particulares',
      'Empresas',
      'Personas y empresas',
      'Todavía no estoy seguro',
    ],
  ),
  QuizQuestion(
    text: '¿Qué comprobante necesitas emitir?',
    options: [
      'Boleta de venta',
      'Factura',
      'Ambos',
      'No emito comprobantes todavía',
      'No estoy seguro',
    ],
  ),
  _incomeQuestion,
  QuizQuestion(
    text: '¿Tendrás gastos frecuentes relacionados con el servicio?',
    options: [
      'Combustible y mantenimiento',
      'Alquiler del vehículo',
      'Comisiones de aplicaciones',
      'Todos los anteriores',
      'No estoy seguro',
    ],
  ),
];

const _deliveryQuestions = <QuizQuestion>[
  QuizQuestion(
    text: '¿Cómo trabajas actualmente?',
    options: [
      'Con una sola plataforma',
      'Con varias plataformas',
      'Realizo entregas directamente',
      'Plataformas y clientes directos',
      'Recién voy a comenzar',
    ],
  ),
  QuizQuestion(
    text: '¿Cómo recibes el detalle de tus ingresos?',
    options: [
      'Reporte o PDF de la plataforma',
      'Resumen dentro de la aplicación',
      'Transferencias sin reporte',
      'No recibo ningún resumen',
      'Aún no lo sé',
    ],
  ),
  QuizQuestion(
    text: '¿Emites algún comprobante actualmente?',
    options: [
      'Boleta de venta',
      'Factura',
      'Ambos',
      'No emito comprobantes todavía',
      'No estoy seguro',
    ],
  ),
  _incomeQuestion,
  QuizQuestion(
    text: '¿Atiendes también pedidos fuera de las plataformas?',
    options: [
      'Sí, con frecuencia',
      'Sí, ocasionalmente',
      'No, solo trabajo con plataformas',
      'Todavía no',
    ],
  ),
];

const _professionalQuestions = <QuizQuestion>[
  QuizQuestion(
    text: '¿Cómo prestas tus servicios?',
    options: [
      'De forma independiente',
      'Trabajo para una empresa en planilla',
      'Trabajo en planilla y también por mi cuenta',
      'Recién voy a comenzar',
    ],
  ),
  QuizQuestion(
    text: '¿A quiénes brindas principalmente el servicio?',
    options: [
      'Empresas',
      'Personas',
      'Empresas y personas',
      'Aún no estoy seguro',
    ],
  ),
  QuizQuestion(
    text: '¿Emitirás recibos por honorarios?',
    options: [
      'Sí',
      'No',
      'Todavía no',
      'No sé cómo funciona',
    ],
  ),
  QuizQuestion(
    text: '¿Cuánto estimas recibir al mes por tus servicios?',
    options: [
      'Hasta S/ 1,500',
      'De S/ 1,501 a S/ 4,000',
      'De S/ 4,001 a S/ 8,000',
      'Más de S/ 8,000',
      'Aún no puedo estimarlo',
    ],
  ),
  QuizQuestion(
    text: '¿Algún cliente te aplicará retención de cuarta categoría?',
    options: [
      'Sí',
      'No',
      'No estoy seguro',
      'Todavía no tengo clientes',
    ],
  ),
];

/// Preguntas segun la actividad elegida en registro-5.
List<QuizQuestion> questionsForActivity(String? activity) {
  switch (activity) {
    case 'DELIVERY_PEYA':
      return _deliveryQuestions;
    case 'SERVICIOS_PROFESIONALES':
      return _professionalQuestions;
    case 'TAXISTA':
    default:
      return _taxiQuestions;
  }
}
