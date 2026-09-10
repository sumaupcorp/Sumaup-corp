import 'package:dio/dio.dart';
import '../../../core/errors/app_exception.dart';
import '../domain/chat_models.dart';

class ChatbotRepository {
  ChatbotRepository(this._dio);
  final Dio _dio;

  Future<ChatReply> send({String? conversationId, required String text}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/chatbot/messages', data: {
        'conversationId': conversationId,
        'text': text,
      });
      return ChatReply.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<AiUsage> usage() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/chatbot/usage');
      return AiUsage.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<List<ChatMessage>> messages(String conversationId) async {
    try {
      final res = await _dio.get<List<dynamic>>('/chatbot/conversations/$conversationId/messages');
      return (res.data ?? []).map((e) => ChatMessage.fromJson(e as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
