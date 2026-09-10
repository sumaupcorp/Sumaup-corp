import 'package:dio/dio.dart';
import '../../../core/errors/app_exception.dart';
import '../domain/ruc_fiscal.dart';
import '../domain/sol_credentials.dart';

class FiscalRepository {
  FiscalRepository(this._dio);
  final Dio _dio;

  Future<SolCredentials> getSol() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/fiscal/sol');
      return SolCredentials.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<SolCredentials> upsertSol({
    String? docMode,
    String? ruc,
    String? solUser,
    String? dni,
    String? solPass,
  }) async {
    try {
      final res = await _dio.put<Map<String, dynamic>>('/app/fiscal/sol', data: {
        'docMode': docMode,
        'ruc': ruc,
        'solUser': solUser,
        'dni': dni,
        'solPass': solPass,
      });
      return SolCredentials.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Valida la Clave SOL con un login real en SUNAT (no guarda nada). Puede tardar
  /// (Playwright inicia sesion). docMode: 'ruc' (ruc+usuario+clave) o 'dni' (dni+clave).
  Future<SolValidation> validateSol({
    required String docMode,
    String? ruc,
    String? solUser,
    String? dni,
    required String solPass,
  }) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/fiscal/sol/validate', data: {
        'docMode': docMode,
        'ruc': ruc,
        'solUser': solUser,
        'dni': dni,
        'solPass': solPass,
      });
      return SolValidation.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Datos de la Ficha RUC ya guardados en el perfil.
  Future<RucFiscal> getRuc() async {
    try {
      final res = await _dio.get<Map<String, dynamic>>('/app/fiscal/ruc');
      return RucFiscal.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Consulta SUNAT (microservicio) y guarda los datos en el perfil. Puede tardar
  /// (el scraper resuelve un captcha). Si ruc es null, usa el del perfil.
  Future<RucFiscal> refreshRuc({String? ruc}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/fiscal/ruc-refresh', data: {'ruc': ruc});
      return RucFiscal.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  /// Consulta por DNI (flujo usual: la persona no recuerda su RUC). Resuelve el RUC
  /// y guarda los datos en el perfil. docType: 1=DNI,4=CE,7=Pasaporte,A=Ced.Diplomatica.
  Future<RucFiscal> refreshByDni({required String dni, String docType = '1'}) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>('/app/fiscal/dni-refresh',
          data: {'dni': dni, 'docType': docType});
      return RucFiscal.fromJson(res.data ?? {});
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
