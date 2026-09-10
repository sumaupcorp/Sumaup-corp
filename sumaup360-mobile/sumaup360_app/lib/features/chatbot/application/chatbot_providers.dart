import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers.dart';
import '../data/chatbot_repository.dart';

final chatbotRepositoryProvider =
    Provider<ChatbotRepository>((ref) => ChatbotRepository(ref.watch(dioProvider)));
