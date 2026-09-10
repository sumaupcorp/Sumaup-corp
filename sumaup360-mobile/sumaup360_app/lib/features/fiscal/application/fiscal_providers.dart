import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/fiscal_repository.dart';
import '../domain/ruc_fiscal.dart';
import '../domain/sol_credentials.dart';

final fiscalRepositoryProvider = Provider<FiscalRepository>((ref) => FiscalRepository(ref.watch(dioProvider)));

/// Estado de la conexion SUNAT del usuario (enmascarado).
final solCredentialsProvider = FutureProvider<SolCredentials>((ref) {
  return ref.watch(fiscalRepositoryProvider).getSol();
});

/// Datos de la Ficha RUC guardados en el perfil.
final rucFiscalProvider = FutureProvider<RucFiscal>((ref) {
  return ref.watch(fiscalRepositoryProvider).getRuc();
});
