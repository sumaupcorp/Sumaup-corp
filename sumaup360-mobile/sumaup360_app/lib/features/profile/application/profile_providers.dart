import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/profile_repository.dart';
import '../domain/profile_me.dart';

final profileRepositoryProvider = Provider<ProfileRepository>((ref) => ProfileRepository(ref.watch(dioProvider)));

final profileMeProvider = FutureProvider<ProfileMe>((ref) {
  return ref.watch(profileRepositoryProvider).getMe();
});
