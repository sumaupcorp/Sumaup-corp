import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers.dart';
import '../data/honorarios_repository.dart';
import '../domain/honorarios_models.dart';

final honorariosRepositoryProvider =
    Provider<HonorariosRepository>((ref) => HonorariosRepository(ref.watch(dioProvider)));

final honorariosProvider = FutureProvider<List<HonorarioRequestModel>>(
    (ref) => ref.watch(honorariosRepositoryProvider).honorarios());

final suspensionesProvider = FutureProvider<List<SuspensionRequestModel>>(
    (ref) => ref.watch(honorariosRepositoryProvider).suspensiones());
