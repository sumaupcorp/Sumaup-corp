import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/storage/secure_storage_service.dart';
import '../../../core/utils/validators.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../../shared/widgets/secondary_button.dart';
import '../application/fiscal_providers.dart';
import '../domain/ruc_fiscal.dart';
import '../domain/sol_credentials.dart';

/// Pantalla para conectar la cuenta SUNAT (RUC + usuario y Clave SOL).
class SunatConnectScreen extends ConsumerStatefulWidget {
  const SunatConnectScreen({super.key});

  @override
  ConsumerState<SunatConnectScreen> createState() => _SunatConnectScreenState();
}

class _SunatConnectScreenState extends ConsumerState<SunatConnectScreen> {
  static const _kIntroSeen = 'sunat_intro_seen';

  final _formKey = GlobalKey<FormState>();
  final _ruc = TextEditingController();
  final _solUser = TextEditingController();
  final _solDni = TextEditingController();
  final _pass = TextEditingController();
  final _docNumber = TextEditingController();
  final _storage = SecureStorageService();
  String _authMode = 'ruc'; // metodo de login SUNAT: 'ruc' (ruc+usuario) o 'dni'
  String _docMode = 'dni'; // buscador de RUC: 'dni' | 'ruc'
  bool _obscure = true;
  bool _loading = false;
  bool _prefilled = false;
  bool _rucPrefilled = false;
  bool _consulting = false;
  bool _validating = false;
  SolValidation? _validation;
  RucFiscal? _rucData;
  String? _error;

  // Intro informativa de primera vez.
  bool _introChecked = false;
  bool _showIntro = false;

  @override
  void initState() {
    super.initState();
    _checkIntro();
    // Si el usuario cambia la clave, la validacion previa deja de ser valida.
    _pass.addListener(_resetValidation);
  }

  void _resetValidation() {
    if (_validation != null && mounted) setState(() => _validation = null);
  }

  Future<void> _checkIntro() async {
    final seen = await _storage.read(_kIntroSeen);
    if (!mounted) return;
    setState(() {
      _showIntro = seen != 'true';
      _introChecked = true;
    });
  }

  Future<void> _finishIntro() async {
    await _storage.write(_kIntroSeen, 'true');
    if (mounted) setState(() => _showIntro = false);
  }

  @override
  void dispose() {
    _ruc.dispose();
    _solUser.dispose();
    _solDni.dispose();
    _pass.dispose();
    _docNumber.dispose();
    super.dispose();
  }

  void _prefill(SolCredentials c) {
    if (_prefilled) return;
    _prefilled = true;
    _ruc.text = c.ruc ?? '';
    _solUser.text = c.solUser ?? '';
    _solDni.text = c.dni ?? '';
    if (c.docMode == 'dni' || c.docMode == 'ruc') _authMode = c.docMode!;
  }

  /// Si el RUC no vino en las credenciales, lo toma de la Ficha RUC del perfil
  /// (ya resuelta en el diagnostico). Una sola vez, sin pisar lo que el usuario escriba.
  void _maybePrefillRuc(String? ruc) {
    if (_rucPrefilled) return;
    if (_ruc.text.trim().isEmpty && ruc != null && ruc.isNotEmpty) {
      _rucPrefilled = true;
      _ruc.text = ruc;
    }
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      final repo = ref.read(fiscalRepositoryProvider);
      final pass = _pass.text.isEmpty ? null : _pass.text;
      if (_authMode == 'dni') {
        await repo.upsertSol(docMode: 'dni', dni: _solDni.text.trim(), solPass: pass);
      } else {
        await repo.upsertSol(
            docMode: 'ruc', ruc: _ruc.text.trim(), solUser: _solUser.text.trim(), solPass: pass);
      }
      ref.invalidate(solCredentialsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cuenta SUNAT guardada de forma segura.')),
      );
      Navigator.of(context).pop();
    } on AppException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'No se pudo guardar. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  /// Paso previo: valida la Clave SOL con un login real en SUNAT (modo RUC).
  Future<void> _validate() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _validating = true;
      _validation = null;
    });
    try {
      final repo = ref.read(fiscalRepositoryProvider);
      final v = _authMode == 'dni'
          ? await repo.validateSol(docMode: 'dni', dni: _solDni.text.trim(), solPass: _pass.text)
          : await repo.validateSol(
              docMode: 'ruc', ruc: _ruc.text.trim(), solUser: _solUser.text.trim(), solPass: _pass.text);
      if (!mounted) return;
      setState(() {
        _validation = v;
        if (!v.ok) _error = v.detail ?? 'No pudimos validar tu Clave SOL.';
      });
    } on AppException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'No se pudo validar. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _validating = false);
    }
  }

  Future<void> _consultar() async {
    final doc = _docNumber.text.trim();
    final isDni = _docMode == 'dni';
    if (isDni && doc.length != 8) {
      setState(() => _error = 'Ingresa un DNI valido de 8 digitos.');
      return;
    }
    if (!isDni && doc.length != 11) {
      setState(() => _error = 'Ingresa un RUC valido de 11 digitos.');
      return;
    }
    setState(() {
      _error = null;
      _consulting = true;
    });
    try {
      final repo = ref.read(fiscalRepositoryProvider);
      final data = isDni ? await repo.refreshByDni(dni: doc) : await repo.refreshRuc(ruc: doc);
      ref.invalidate(rucFiscalProvider);
      if (!mounted) return;
      setState(() {
        _rucData = data;
        // Si encontro el RUC, lo prellena en el campo de credenciales.
        if (data.ruc != null && data.ruc!.isNotEmpty) _ruc.text = data.ruc!;
      });
    } on AppException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'No se pudo consultar SUNAT. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _consulting = false);
    }
  }

  /// Area de accion: valida primero y, una vez correcta la Clave SOL, permite encriptar y guardar.
  Widget _actionArea(bool connected) {
    final validated = _validation?.ok == true;

    // Editando una conexion existente sin cambiar la clave: guardar directo.
    if (connected && _pass.text.isEmpty) {
      return PrimaryButton(label: 'Actualizar conexion', loading: _loading, onPressed: _submit);
    }

    if (validated) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _ValidatedCard(nombre: _validation?.nombre),
          const SizedBox(height: AppSpacing.md),
          PrimaryButton(label: 'Encriptar y guardar', loading: _loading, onPressed: _submit),
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        PrimaryButton(
          label: _validating ? 'Validando en SUNAT...' : 'Validar en SUNAT',
          loading: _validating,
          onPressed: _validate,
        ),
        const SizedBox(height: AppSpacing.sm),
        const Text(
          'Verificamos tu Clave SOL iniciando sesion en SUNAT. Puede tardar unos segundos.',
          style: AppText.small,
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    // Mientras leemos la bandera de intro, no parpadeamos el formulario.
    if (!_introChecked) {
      return const Scaffold(
        backgroundColor: AppColors.surfaceAlt,
        body: Center(child: CircularProgressIndicator(color: AppColors.primary)),
      );
    }
    if (_showIntro) {
      return _SunatIntroView(onDone: _finishIntro);
    }

    final async = ref.watch(solCredentialsProvider);
    final connected = async.valueOrNull?.isConnected ?? false;
    if (async.hasValue) _prefill(async.value!);
    // Prellena el RUC con el de la Ficha RUC del perfil si aun no hay ninguno.
    _maybePrefillRuc(ref.watch(rucFiscalProvider).valueOrNull?.ruc);

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(
        backgroundColor: AppColors.surfaceAlt,
        title: const Text('Conecta tu SUNAT'),
        actions: [
          IconButton(
            tooltip: 'Como funciona',
            icon: const Icon(Icons.help_outline, color: AppColors.muted),
            onPressed: () => setState(() => _showIntro = true),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.xxl),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _Hero(),
                const SizedBox(height: AppSpacing.lg),
                if (connected) ...[
                  const _ConnectedBanner(),
                  const SizedBox(height: AppSpacing.lg),
                ],
                const Text('Como inicias sesion en SUNAT', style: AppText.title),
                const SizedBox(height: 4),
                const Text('Elige el metodo que usas en SUNAT: por RUC (con usuario) o por DNI.',
                    style: AppText.small),
                const SizedBox(height: AppSpacing.md),
                _DocModeToggle(
                  mode: _authMode,
                  onChanged: (m) => setState(() {
                    _authMode = m;
                    _validation = null;
                    _error = null;
                  }),
                ),
                const SizedBox(height: AppSpacing.lg),
                if (_authMode == 'ruc') ...[
                  AppTextField(
                    label: 'RUC',
                    controller: _ruc,
                    hint: '10XXXXXXXXX',
                    keyboardType: TextInputType.number,
                    prefixIcon: 'id',
                    maxLength: 11,
                    validator: (v) => Validators.required(v, field: 'El RUC'),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  AppTextField(
                    label: 'Usuario SOL',
                    controller: _solUser,
                    hint: 'Tu usuario de Clave SOL',
                    prefixIcon: 'user',
                    validator: (v) => Validators.required(v, field: 'El usuario SOL'),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                ] else ...[
                  AppTextField(
                    label: 'DNI',
                    controller: _solDni,
                    hint: '8 digitos',
                    keyboardType: TextInputType.number,
                    prefixIcon: 'id',
                    maxLength: 8,
                    validator: (v) =>
                        (v == null || v.trim().length != 8) ? 'Ingresa un DNI de 8 digitos.' : null,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                ],
                AppTextField(
                  label: 'Clave SOL',
                  controller: _pass,
                  obscure: _obscure,
                  hint: connected ? 'Dejala vacia para no cambiarla' : 'Tu Clave SOL',
                  prefixIcon: 'lock',
                  suffix: IconButton(
                    icon: AppIcon(_obscure ? 'eye-off' : 'eye', color: AppColors.muted),
                    onPressed: () => setState(() => _obscure = !_obscure),
                  ),
                  validator: (v) {
                    // Si ya esta conectado, permitir guardar sin reescribir la clave.
                    if (connected) return null;
                    return Validators.required(v, field: 'La Clave SOL');
                  },
                ),
                if (_error != null) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(_error!, style: const TextStyle(color: AppColors.danger)),
                ],
                const SizedBox(height: AppSpacing.lg),
                const _SecurityNote(),
                const SizedBox(height: AppSpacing.xl),
                _actionArea(connected),
                const SizedBox(height: AppSpacing.lg),
                // Ayuda secundaria: la mayoria ya trae su RUC del diagnostico. Quien no lo
                // recuerde puede buscarlo aqui sin saturar el flujo principal.
                Theme(
                  data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
                  child: ExpansionTile(
                    tilePadding: EdgeInsets.zero,
                    childrenPadding: const EdgeInsets.only(bottom: AppSpacing.sm),
                    title: const Text('No recuerdo mi RUC', style: AppText.bodyStrong),
                    subtitle: const Text('Buscalo en SUNAT con tu DNI o RUC', style: AppText.small),
                    children: [
                      _DocModeToggle(mode: _docMode, onChanged: (m) => setState(() => _docMode = m)),
                      const SizedBox(height: AppSpacing.md),
                      AppTextField(
                        label: _docMode == 'dni' ? 'DNI' : 'RUC',
                        controller: _docNumber,
                        keyboardType: TextInputType.number,
                        prefixIcon: _docMode == 'dni' ? 'id' : 'receipt-tax',
                        hint: _docMode == 'dni' ? '8 digitos' : '11 digitos',
                        maxLength: _docMode == 'dni' ? 8 : 11,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      SecondaryButton(
                        label: _consulting ? 'Consultando SUNAT...' : 'Consultar en SUNAT',
                        icon: _consulting
                            ? const SizedBox(
                                width: 18, height: 18,
                                child: CircularProgressIndicator(strokeWidth: 2.2, color: AppColors.primary))
                            : const AppIcon('search', size: 20, color: AppColors.primary),
                        onPressed: _consulting ? null : _consultar,
                      ),
                      Builder(builder: (_) {
                        final data = _rucData ?? ref.watch(rucFiscalProvider).valueOrNull;
                        if (data == null || !data.hasData) return const SizedBox.shrink();
                        return Padding(
                          padding: const EdgeInsets.only(top: AppSpacing.md),
                          child: _RucDataCard(data: data),
                        );
                      }),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero();
  static const _indigo = Color(0xFF161A40);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [_indigo, AppColors.primary],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: AppShadows.primary,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(13)),
            alignment: Alignment.center,
            child: const AppIcon('lock', size: 26, color: Colors.white),
          ),
          const SizedBox(height: AppSpacing.lg),
          Text('Conecta tu cuenta SUNAT', style: AppText.h2.copyWith(color: Colors.white)),
          const SizedBox(height: AppSpacing.sm),
          Text(
            'Con tu Clave SOL, Suma puede procesar tus comprobantes y mantener tu orden tributario al dia.',
            style: AppText.body.copyWith(color: Colors.white.withValues(alpha: 0.9)),
          ),
        ],
      ),
    );
  }
}

class _ConnectedBanner extends StatelessWidget {
  const _ConnectedBanner();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.success.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(AppSpacing.radius),
        border: Border.all(color: AppColors.success.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          const AppIcon('check-circle', size: 20, color: AppColors.success),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Tu cuenta SUNAT ya esta conectada. Puedes actualizar tus datos cuando quieras.',
              style: AppText.small.copyWith(color: AppColors.success, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta de exito tras validar la Clave SOL contra SUNAT.
class _ValidatedCard extends StatelessWidget {
  const _ValidatedCard({this.nombre});
  final String? nombre;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.success.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(AppSpacing.radius),
        border: Border.all(color: AppColors.success.withValues(alpha: 0.4)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AppIcon('check-circle', size: 22, color: AppColors.success),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Clave SOL validada', style: AppText.bodyStrong.copyWith(color: AppColors.success)),
                if ((nombre ?? '').isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(nombre!, style: AppText.small),
                ],
                const SizedBox(height: 2),
                const Text('Presiona Encriptar y guardar para conectar tu cuenta.', style: AppText.small),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SecurityNote extends StatelessWidget {
  const _SecurityNote();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radius),
        boxShadow: AppShadows.card,
      ),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(10)),
            alignment: Alignment.center,
            child: const AppIcon('lock', size: 20, color: AppColors.primary),
          ),
          const SizedBox(width: AppSpacing.md),
          const Expanded(
            child: Text(
              'Tu Clave SOL se guarda cifrada y nunca se muestra. Solo se usa para procesar tu informacion tributaria.',
              style: AppText.small,
            ),
          ),
        ],
      ),
    );
  }
}

/// Segmentado para elegir buscar por DNI o por RUC.
class _DocModeToggle extends StatelessWidget {
  const _DocModeToggle({required this.mode, required this.onChanged});
  final String mode;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: AppColors.surfaceAlt,
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          _seg('Por DNI', 'dni'),
          _seg('Por RUC', 'ruc'),
        ],
      ),
    );
  }

  Widget _seg(String label, String value) {
    final active = mode == value;
    return Expanded(
      child: GestureDetector(
        onTap: () => onChanged(value),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          padding: const EdgeInsets.symmetric(vertical: 9),
          decoration: BoxDecoration(
            color: active ? AppColors.primary : Colors.transparent,
            borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
          ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: 13,
              color: active ? Colors.white : AppColors.body,
            ),
          ),
        ),
      ),
    );
  }
}

/// Contenido de una diapositiva del intro.
class _IntroSlide {
  const _IntroSlide(this.icon, this.title, this.body);
  final String icon;
  final String title;
  final String body;
}

/// Vista informativa de primera vez: explica para que sirve conectar SUNAT.
/// Se puede saltar en cualquier momento; al terminar lleva al formulario.
class _SunatIntroView extends StatefulWidget {
  const _SunatIntroView({required this.onDone});
  final VoidCallback onDone;

  @override
  State<_SunatIntroView> createState() => _SunatIntroViewState();
}

class _SunatIntroViewState extends State<_SunatIntroView> {
  final _controller = PageController();
  int _page = 0;

  static const _slides = <_IntroSlide>[
    _IntroSlide(
      'receipt-tax',
      'Tu SUNAT en piloto automatico',
      'Con tu Clave SOL, el equipo de SUMAUP360 concilia tus movimientos, presenta tus declaraciones y genera tus boletas y facturas por ti. Funciona seas taxista, repartidor o profesional independiente.',
    ),
    _IntroSlide(
      'lock',
      'Tus datos, siempre cifrados',
      'Tu Clave SOL se guarda cifrada y nunca se muestra. Solo se usa para realizar tus tramites tributarios; nadie mas puede verla.',
    ),
    _IntroSlide(
      'check-circle',
      'Validamos tu acceso',
      'Verificamos que tu Clave SOL funcione. Cuando quede validada veras tu cuenta marcada como conectada y lista para operar.',
    ),
    _IntroSlide(
      'id',
      'Solo un paso',
      'Ya tenemos tu RUC de tu diagnostico. Ingresa tu usuario y Clave SOL, presiona Encriptar y conectar, y listo.',
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  bool get _isLast => _page == _slides.length - 1;

  void _next() {
    if (_isLast) {
      widget.onDone();
    } else {
      _controller.nextPage(duration: const Duration(milliseconds: 260), curve: Curves.easeOut);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      body: SafeArea(
        child: Column(
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: widget.onDone,
                child: const Text('Saltar', style: TextStyle(color: AppColors.muted, fontWeight: FontWeight.w600)),
              ),
            ),
            Expanded(
              child: PageView.builder(
                controller: _controller,
                onPageChanged: (i) => setState(() => _page = i),
                itemCount: _slides.length,
                itemBuilder: (_, i) => _SlideView(slide: _slides[i]),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(_slides.length, (i) {
                final active = i == _page;
                return AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: active ? 22 : 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: active ? AppColors.primary : AppColors.border,
                    borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                  ),
                );
              }),
            ),
            Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: PrimaryButton(
                label: _isLast ? 'Ingresar mis datos' : 'Siguiente',
                onPressed: _next,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SlideView extends StatelessWidget {
  const _SlideView({required this.slide});
  final _IntroSlide slide;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xl),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(20)),
            alignment: Alignment.center,
            child: AppIcon(slide.icon, size: 36, color: AppColors.primary),
          ),
          const SizedBox(height: AppSpacing.xl),
          Text(slide.title, style: AppText.h2),
          const SizedBox(height: AppSpacing.md),
          Text(slide.body, style: AppText.body.copyWith(color: AppColors.muted, height: 1.5)),
        ],
      ),
    );
  }
}

/// Tarjeta con los datos de la Ficha RUC traidos de SUNAT.
class _RucDataCard extends StatelessWidget {
  const _RucDataCard({required this.data});
  final RucFiscal data;

  @override
  Widget build(BuildContext context) {
    final activity = data.economicActivity == null
        ? null
        : (data.ciiuCode != null ? '${data.ciiuCode} - ${data.economicActivity}' : data.economicActivity);
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              AppIcon('circle-dashed-percentage', size: 20, color: AppColors.primary),
              SizedBox(width: 8),
              Text('Tus datos en SUNAT', style: AppText.title),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          // RUC resuelto + razon social, destacado.
          if (data.ruc != null && data.ruc!.isNotEmpty)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(AppSpacing.md),
              margin: const EdgeInsets.only(bottom: AppSpacing.md),
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(AppSpacing.radius),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const AppIcon('receipt-tax', size: 18, color: AppColors.primary),
                      const SizedBox(width: 6),
                      Text('RUC ${data.ruc}',
                          style: AppText.bodyStrong.copyWith(color: AppColors.primary)),
                    ],
                  ),
                  if (data.razonSocial != null && data.razonSocial!.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(data.razonSocial!, style: AppText.small),
                  ],
                ],
              ),
            ),
          _row('Estado', data.taxStatus),
          _row('Condicion', data.taxCondition),
          _row('Tipo de contribuyente', data.taxpayerType),
          _row('Actividad economica', activity),
          _row('Fecha de inscripcion', data.fechaInscripcion),
          _row('Domicilio fiscal', data.domicilioFiscal),
        ],
      ),
    );
  }

  Widget _row(String label, String? value) {
    // Oculta campos vacios o "-" (SUNAT no siempre los provee).
    if (value == null || value.trim().isEmpty || value.trim() == '-') {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: AppText.small),
          const SizedBox(height: 2),
          Text(value, style: AppText.bodyStrong),
        ],
      ),
    );
  }
}
