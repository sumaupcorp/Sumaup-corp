import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/peya_repository.dart';
import '../domain/peya_upload.dart';

final peyaRepositoryProvider = Provider<PeyaRepository>((ref) => PeyaRepository(ref.watch(dioProvider)));

final peyaUploadsProvider = FutureProvider<List<PeyaUpload>>((ref) => ref.watch(peyaRepositoryProvider).uploads());

final peyaFilesProvider =
    FutureProvider.family<List<PeyaFile>, String>((ref, id) => ref.watch(peyaRepositoryProvider).files(id));
