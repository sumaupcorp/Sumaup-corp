import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/taxi_repository.dart';
import '../domain/attachment.dart';
import '../domain/receipt_request.dart';

final taxiRepositoryProvider = Provider<TaxiRepository>((ref) => TaxiRepository(ref.watch(dioProvider)));

final qrTokenProvider = FutureProvider<String>((ref) => ref.watch(taxiRepositoryProvider).qrToken());

final taxiRequestsProvider =
    FutureProvider<List<ReceiptRequestModel>>((ref) => ref.watch(taxiRepositoryProvider).requests());

/// Adjuntos (comprobantes generados) de una solicitud, por id.
final taxiAttachmentsProvider = FutureProvider.family<List<AttachmentModel>, String>(
  (ref, requestId) => ref.watch(taxiRepositoryProvider).requestAttachments(requestId),
);
