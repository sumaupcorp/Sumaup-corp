/// Gasto del usuario (mapea ExpenseResponse del backend).
class Expense {
  Expense({
    required this.id,
    required this.txDate,
    required this.amount,
    required this.currency,
    this.category,
    this.paymentMethod,
    this.description,
    this.receiptId,
  });

  final String id;
  final DateTime txDate;
  final double amount;
  final String currency;
  final String? category;
  final String? paymentMethod;
  final String? description;
  final String? receiptId;

  factory Expense.fromJson(Map<String, dynamic> j) => Expense(
        id: j['id'] as String,
        txDate: DateTime.parse(j['txDate'] as String),
        amount: (j['amount'] as num).toDouble(),
        currency: (j['currency'] as String?) ?? 'PEN',
        category: j['category'] as String?,
        paymentMethod: j['paymentMethod'] as String?,
        description: j['description'] as String?,
        receiptId: j['receiptId'] as String?,
      );
}

/// Categorias de gasto de la Linea Personas.
const expenseCategories = <String>[
  'Combustible', 'Movilidad', 'Alimentacion', 'Mantenimiento',
  'Servicios', 'Alquiler', 'Insumos', 'Otros',
];
