import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'network/api_client.dart';

/// Cliente Dio compartido (con interceptor de Firebase idToken).
final dioProvider = Provider<Dio>((ref) => ApiClient().dio);
