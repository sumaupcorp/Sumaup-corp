import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../../shared/widgets/info_title.dart';
import '../../../shared/widgets/money_total_card.dart';
import '../../../shared/widgets/suma_image.dart';
import '../../../shared/widgets/transaction_tile.dart';
import '../../auth/application/auth_providers.dart';
import '../../expenses/application/expense_providers.dart';
import '../../expenses/presentation/expense_screen.dart';
import '../../fiscal/application/fiscal_providers.dart';
import '../../income/application/income_providers.dart';
import '../../income/presentation/income_screen.dart';
import '../../plans/domain/plan_catalog.dart';
import '../../access/feature_access.dart';
import '../../chatbot/presentation/chat_screen.dart';
import '../../profile/application/profile_providers.dart';
import '../../profile/domain/profile_me.dart';
import '../../peya/presentation/peya_screen.dart';
import '../../taxi/presentation/comprobantes_screen.dart';
import '../../honorarios/presentation/servicios_screen.dart';
import '../application/summary_providers.dart';

/// Contenedor principal con tabs y navbar flotante (estilo tarjeta).
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int _tab = 0;

  void _goTab(int i) => setState(() => _tab = i);

  @override
  Widget build(BuildContext context) {
    final profile = ref.watch(profileMeProvider).valueOrNull;
    // Tipo de modulo del 4o tab segun el rol: taxi (Comprobantes) | peya | servicios.
    final String kind = profile?.isDeliveryPeya == true
        ? 'peya'
        : profile?.isServiciosProfesionales == true
            ? 'servicios'
            : 'taxi';
    final (String moduleIcon, String moduleLabel) = switch (kind) {
      'peya' => ('arrow-bar-to-down', 'Peya'),
      'servicios' => ('notes', 'Servicios'),
      _ => ('receipt', 'Comprobantes'),
    };
    final tabs = [
      _HomeTab(onOpenProfile: () => context.push(Routes.account)),
      const IncomeScreen(embedded: true),
      const ExpenseScreen(embedded: true),
      _ModuleTab(kind: kind),
      const ChatScreen(embedded: true),
    ];
    final navItems = <(String, String)>[
      ('home', 'Inicio'),
      ('trending-up', 'Ingresos'),
      ('trending-down', 'Gastos'),
      (moduleIcon, moduleLabel),
      ('help-small', 'Chat'),
    ];
    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      body: SafeArea(bottom: false, child: tabs[_tab]),
      bottomNavigationBar: _FloatingNavBar(current: _tab, onTap: _goTab, items: navItems),
    );
  }
}

/// Navbar flotante con bordes redondeados (tipo Rappi).
class _FloatingNavBar extends StatelessWidget {
  const _FloatingNavBar({required this.current, required this.onTap, required this.items});
  final int current;
  final ValueChanged<int> onTap;
  final List<(String, String)> items; // (nombre de icono, etiqueta)

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        margin: const EdgeInsets.fromLTRB(AppSpacing.lg, 0, AppSpacing.lg, AppSpacing.md),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: AppSpacing.sm),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(26),
          boxShadow: AppShadows.floating,
        ),
        child: Row(
          children: List.generate(items.length, (i) {
            final selected = i == current;
            final (iconName, label) = items[i];
            return Expanded(
              child: InkWell(
                onTap: () => onTap(i),
                borderRadius: BorderRadius.circular(AppSpacing.radius),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      padding: EdgeInsets.symmetric(
                        horizontal: selected ? 18 : 12,
                        vertical: 7,
                      ),
                      decoration: BoxDecoration(
                        color: selected ? AppColors.primarySoft : Colors.transparent,
                        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
                      ),
                      child: AppIcon(iconName, size: 23, color: selected ? AppColors.primary : AppColors.muted),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      label,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                        color: selected ? AppColors.primary : AppColors.muted,
                      ),
                    ),
                  ],
                ),
              ),
            );
          }),
        ),
      ),
    );
  }
}

class _HomeTab extends ConsumerWidget {
  const _HomeTab({required this.onOpenProfile});
  final VoidCallback onOpenProfile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final email = ref.read(authControllerProvider).currentUser?.email ?? '';
    final profile = ref.watch(profileMeProvider).valueOrNull;
    final name = _firstName(profile, email);
    final verified = ref.read(authControllerProvider).isEmailVerified;
    final summary = ref.watch(summaryProvider);

    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () async {
        ref.invalidate(summaryProvider);
        ref.invalidate(incomeListProvider);
        ref.invalidate(expenseListProvider);
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
        children: [
          _Header(name: name, photoUrl: profile?.photoUrl, onOpenProfile: onOpenProfile),
          if (!verified) ...[
            const SizedBox(height: AppSpacing.lg),
            const _Banner(text: 'Verifica tu correo para desbloquear todas las funciones.'),
          ],
          if (profile?.orientationPending == true) ...[
            const SizedBox(height: AppSpacing.lg),
            const _OrientationPendingCard(),
          ],
          const SizedBox(height: AppSpacing.lg),
          summary.when(
            loading: () => const _HeroSkeleton(),
            error: (_, __) => const _HeroBalanceCard(income: 0, expense: 0, balance: 0),
            data: (s) => _HeroBalanceCard(income: s.totalIncome, expense: s.totalExpense, balance: s.utility),
          ),
          const _SunatCard(),
          const SizedBox(height: AppSpacing.xl),
          const InfoTitle(
            'Acciones rapidas',
            info: 'Accesos directos para registrar tus ingresos, gastos y boletas, o ver tus estadisticas.',
          ),
          const SizedBox(height: AppSpacing.md),
          const _QuickActions(),
          const SizedBox(height: AppSpacing.xl),
          const _ProUpgradeCard(),
          // Orientacion tributaria: si esta PENDING ya se muestra el banner de
          // arriba; si la completo puede VER su resultado guardado; si tiene RUC
          // (o cuenta antigua) puede evaluarse/recategorizarse una unica vez.
          if (profile?.orientationPending != true) ...[
            const SizedBox(height: AppSpacing.xl),
            _OrientationCard(completed: profile?.orientationStatus == 'COMPLETED'),
          ],
          const SizedBox(height: AppSpacing.xl),
          const InfoTitle(
            'Ultimos movimientos',
            info: 'Tus ingresos y gastos mas recientes. Toca "Ver todo" para revisar el historial completo.',
            action: 'Ver todo',
          ),
          const SizedBox(height: AppSpacing.md),
          const _RecentMovements(),
        ],
      ),
    );
  }

  String _firstName(ProfileMe? p, String email) {
    if (p?.fullName != null && p!.fullName!.trim().isNotEmpty) {
      return p.fullName!.trim().split(' ').first;
    }
    return email.contains('@') ? email.split('@').first : 'usuario';
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.name, required this.photoUrl, required this.onOpenProfile});
  final String name;
  final String? photoUrl;
  final VoidCallback onOpenProfile;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _Avatar(photoUrl: photoUrl, name: name, size: 48, onTap: onOpenProfile),
        const SizedBox(width: AppSpacing.md),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Hola, $name', style: AppText.h2),
              const SizedBox(height: 2),
              const Text('Asi va tu actividad este mes', style: AppText.small),
            ],
          ),
        ),
        _IconButtonSoft(
          icon: 'bell',
          onTap: () => ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('No tienes notificaciones nuevas.')),
          ),
        ),
      ],
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.photoUrl, required this.name, required this.size, this.onTap});
  final String? photoUrl;
  final String name;
  final double size;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final initials = name.isNotEmpty ? name.characters.first.toUpperCase() : 'U';
    final inner = (photoUrl != null && photoUrl!.isNotEmpty)
        ? ClipOval(
            child: CachedNetworkImage(
              imageUrl: photoUrl!,
              width: size,
              height: size,
              fit: BoxFit.cover,
              errorWidget: (_, __, ___) => _initialsCircle(initials, size),
            ),
          )
        : _initialsCircle(initials, size);
    return GestureDetector(onTap: onTap, child: inner);
  }

  Widget _initialsCircle(String initials, double size) {
    return Container(
      width: size,
      height: size,
      decoration: const BoxDecoration(color: AppColors.primarySoft, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        initials,
        style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700, fontSize: size * 0.4),
      ),
    );
  }
}

class _IconButtonSoft extends StatelessWidget {
  const _IconButtonSoft({required this.icon, required this.onTap});
  final String icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          boxShadow: AppShadows.card,
        ),
        alignment: Alignment.center,
        child: AppIcon(icon, size: 22, color: AppColors.ink),
      ),
    );
  }
}

/// Hero principal: balance del mes con gradiente azul.
class _HeroBalanceCard extends StatelessWidget {
  const _HeroBalanceCard({required this.income, required this.expense, required this.balance});
  final double income;
  final double expense;
  final double balance;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary, AppColors.primaryDark],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: AppShadows.primary,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('Balance del mes', style: AppText.small.copyWith(color: Colors.white.withValues(alpha: 0.85))),
              const Spacer(),
              AppIcon('moneybag', size: 20, color: Colors.white.withValues(alpha: 0.9)),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            Fmt.money(balance),
            style: AppText.display.copyWith(color: Colors.white, fontSize: 32),
          ),
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              _HeroPill(icon: 'arrow-bar-to-down', label: 'Ingresos', value: Fmt.money(income)),
              const SizedBox(width: AppSpacing.md),
              _HeroPill(icon: 'arrow-bar-to-up', label: 'Gastos', value: Fmt.money(expense)),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeroPill extends StatelessWidget {
  const _HeroPill({required this.icon, required this.label, required this.value});
  final String icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(AppSpacing.radius),
        ),
        child: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.2), shape: BoxShape.circle),
              alignment: Alignment.center,
              child: AppIcon(icon, size: 18, color: Colors.white),
            ),
            const SizedBox(width: AppSpacing.sm),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.8))),
                  const SizedBox(height: 2),
                  Text(
                    value,
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: Colors.white),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HeroSkeleton extends StatelessWidget {
  const _HeroSkeleton();
  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 168,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary, AppColors.primaryDark],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: AppShadows.primary,
      ),
      alignment: Alignment.center,
      child: const CircularProgressIndicator(color: Colors.white),
    );
  }
}

/// Card que invita a conectar la cuenta SUNAT (Clave SOL). Se oculta si ya esta conectada.
class _SunatCard extends ConsumerWidget {
  const _SunatCard();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final connected = ref.watch(solCredentialsProvider).valueOrNull?.isConnected ?? false;
    if (connected) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.lg),
      child: GestureDetector(
        onTap: () => context.push(Routes.sunat),
        child: Container(
          padding: const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
            border: Border.all(color: AppColors.primary.withValues(alpha: 0.4)),
            boxShadow: AppShadows.card,
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(13)),
                alignment: Alignment.center,
                child: const AppIcon('lock', size: 24, color: AppColors.primary),
              ),
              const SizedBox(width: AppSpacing.md),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Conecta tu cuenta SUNAT', style: AppText.bodyStrong),
                    SizedBox(height: 2),
                    Text('Agrega tu Clave SOL para automatizar tu orden tributario.',
                        style: AppText.small, maxLines: 2),
                  ],
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              const AppIcon('chevron-right', color: AppColors.muted, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

/// Banner de mejora a plan Pro (estilo Spotify Premium / Rappi Pro).
/// Si el usuario ya tiene diagnostico, muestra su plan recomendado.
class _ProUpgradeCard extends ConsumerWidget {
  const _ProUpgradeCard();

  static const _gold = Color(0xFFF5C046);
  static const _indigo = Color(0xFF161A40);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final segment = ref.watch(profileMeProvider).valueOrNull?.segmentCode;
    final plan = planForSegment(segment);
    final title = plan != null ? 'Activa tu ${plan.name}' : 'Hazte SUMAUP PRO';
    final subtitle = plan != null ? plan.tagline : 'Boletas ilimitadas, alertas y mas.';

    return GestureDetector(
      onTap: () => context.push(Routes.plans),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.lg),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: [_indigo, AppColors.primary],
          ),
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          boxShadow: AppShadows.primary,
        ),
        child: Row(
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(color: _gold.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(13)),
              alignment: Alignment.center,
              child: const AppIcon('crown', size: 26, color: _gold),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: AppText.bodyStrong.copyWith(color: Colors.white), maxLines: 1, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: AppText.small.copyWith(color: Colors.white.withValues(alpha: 0.85)),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const AppIcon('chevron-right', color: Colors.white, size: 20),
          ],
        ),
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.text});
  final String text;
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.warning.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(AppSpacing.radius),
        border: Border.all(color: AppColors.warning.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const AppIcon('mail-exclamation', size: 20, color: AppColors.warning),
        const SizedBox(width: 10),
        Expanded(child: Text(text, style: const TextStyle(color: AppColors.warning, fontWeight: FontWeight.w500))),
      ]),
    );
  }
}

/// Aviso de orientacion tributaria pendiente (el usuario sin RUC la omitio en
/// el registro). Lleva al cuestionario para recibir su recomendacion.
class _OrientationPendingCard extends StatelessWidget {
  const _OrientationPendingCard();

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => context.push(Routes.orientationIntro),
      borderRadius: BorderRadius.circular(AppSpacing.radius),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.md),
        decoration: BoxDecoration(
          color: AppColors.primarySoft,
          borderRadius: BorderRadius.circular(AppSpacing.radius),
          border: Border.all(color: AppColors.primary.withValues(alpha: 0.35)),
        ),
        child: Row(
          children: [
            Image.asset('assets/icons/ic_rayo.png', width: 30, height: 30),
            const SizedBox(width: 10),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Tienes pendiente tu orientacion tributaria',
                    style: TextStyle(color: AppColors.primaryDark, fontWeight: FontWeight.w600),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Responde 5 preguntas y descubre que opcion de RUC te conviene.',
                    style: TextStyle(color: AppColors.primaryDark, fontSize: 12.5),
                  ),
                ],
              ),
            ),
            const AppIcon('square-rounded-arrow-right', size: 20, color: AppColors.primary),
          ],
        ),
      ),
    );
  }
}

/// Tarjeta de orientacion tributaria segun estado:
/// - completada: abre el RESULTADO guardado (no vuelve a analizar).
/// - no realizada (tiene RUC o cuenta antigua): invita a evaluarse; el
///   cuestionario es una sola vez por cuenta (reactivable desde el backoffice).
class _OrientationCard extends StatelessWidget {
  const _OrientationCard({required this.completed});
  final bool completed;

  @override
  Widget build(BuildContext context) {
    final title = completed ? 'Tu orientacion tributaria' : 'Evalua tu situacion tributaria';
    final subtitle = completed
        ? 'Revisa tu resultado, costos estimados y proximos pasos.'
        : 'Responde 5 preguntas y descubre que opcion tributaria te conviene. Disponible una sola vez.';
    final route = completed ? Routes.orientationResult : Routes.orientationIntro;
    return _SoftCard(
      onTap: () => context.push(route),
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        children: [
          const SumaImage(AppAssets.sumaRecomendando, height: 56),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AppText.bodyStrong),
                const SizedBox(height: 4),
                Text(subtitle, style: AppText.small),
              ],
            ),
          ),
          const AppIcon('chevron-right', color: AppColors.muted, size: 20),
        ],
      ),
    );
  }
}

class _QuickActions extends StatelessWidget {
  const _QuickActions();
  @override
  Widget build(BuildContext context) {
    final actions = <(String, String, Color, String)>[
      ('receipt-tax', 'Subir boleta', AppColors.primary, Routes.receiptUpload),
      ('arrow-bar-to-up', 'Registrar gasto', AppColors.danger, Routes.expenseForm),
      ('arrow-bar-to-down', 'Registrar ingreso', AppColors.success, Routes.incomeForm),
      ('brand-google-analytics', 'Estadisticas', AppColors.accent, Routes.stats),
    ];
    return Row(
      children: actions.map((a) {
        return Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xs),
            child: _QuickActionTile(icon: a.$1, label: a.$2, color: a.$3, route: a.$4),
          ),
        );
      }).toList(),
    );
  }
}

class _QuickActionTile extends StatelessWidget {
  const _QuickActionTile({required this.icon, required this.label, required this.color, required this.route});
  final String icon;
  final String label;
  final Color color;
  final String route;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push(route),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(18),
              boxShadow: AppShadows.card,
            ),
            child: Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(12)),
              alignment: Alignment.center,
              child: AppIcon(icon, size: 22, color: color),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(label, textAlign: TextAlign.center, style: AppText.small, maxLines: 2),
        ],
      ),
    );
  }
}

class _RecentMovements extends ConsumerWidget {
  const _RecentMovements();
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final incomes = ref.watch(incomeListProvider).valueOrNull ?? [];
    final expenses = ref.watch(expenseListProvider).valueOrNull ?? [];
    final items = <(DateTime, String, String, Color, String)>[
      ...incomes.map((i) => (i.txDate, i.category ?? 'Ingreso', '+ ${Fmt.money(i.amount)}', AppColors.success, Fmt.date(i.txDate))),
      ...expenses.map((e) => (e.txDate, e.category ?? 'Gasto', '- ${Fmt.money(e.amount)}', AppColors.danger, Fmt.date(e.txDate))),
    ]..sort((a, b) => b.$1.compareTo(a.$1));
    final recent = items.take(5).toList();

    if (recent.isEmpty) return const EmptyHint(text: 'Aun no registras movimientos.');
    return Column(
      children: recent
          .map((m) => TransactionTile(title: m.$2, subtitle: m.$5, amount: m.$3, color: m.$4))
          .toList(),
    );
  }
}

/// Card blanca con sombra suave y borde redondeado (base del look).
class _SoftCard extends StatelessWidget {
  const _SoftCard({required this.child, this.onTap, this.padding});
  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsets? padding;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
      elevation: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        child: Ink(
          padding: padding ?? const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
            boxShadow: AppShadows.card,
          ),
          child: child,
        ),
      ),
    );
  }
}

/// 4o tab: modulo principal segun tipo de cliente (Comprobantes taxista / Peya).
/// FASE 1: preview bloqueado con CTA a Premium. El contenido real llega en FASE 3/4.
/// 4o tab segun tipo de cliente. Taxista: si es Premium muestra Comprobantes real,
/// si no, el preview bloqueado. Peya: preview bloqueado (modulo real en FASE 4).
class _ModuleTab extends ConsumerWidget {
  const _ModuleTab({required this.kind});
  final String kind; // 'taxi' | 'peya' | 'servicios'

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feature = switch (kind) {
      'peya' => 'PEYA_UPLOAD_PDF',
      'servicios' => 'HONORARIOS_RECIBO',
      _ => 'TAXI_COMPROBANTES',
    };
    final access = ref.watch(featureAccessProvider(feature));
    return access.when(
      loading: () => const Center(child: CircularProgressIndicator(color: AppColors.primary)),
      error: (_, __) => _LockedModule(kind: kind),
      data: (a) => a.allowed ? _moduleScreen(kind) : _LockedModule(kind: kind),
    );
  }

  Widget _moduleScreen(String kind) => switch (kind) {
        'peya' => const PeyaScreen(embedded: true),
        'servicios' => const ServiciosScreen(embedded: true),
        _ => const ComprobantesScreen(embedded: true),
      };
}

class _LockedModule extends StatelessWidget {
  const _LockedModule({required this.kind});
  final String kind; // 'taxi' | 'peya' | 'servicios'

  static const _gold = Color(0xFFF5C046);
  static const _indigo = Color(0xFF161A40);

  @override
  Widget build(BuildContext context) {
    final (title, desc, price, bullets) = switch (kind) {
      'peya' => (
          'Peya',
          'Sube tu PDF mensual de ventas y SUMAUP360 prepara tu reporte y tu declaracion.',
          'S/ 29.90',
          const ['Carga de PDF de ventas', 'Reporte y dashboard', 'Declaracion mensual', 'Codigo NPS', 'Alertas SUNAT'],
        ),
      'servicios' => (
          'Servicios',
          'Solicita tus recibos por honorarios y tramita tu suspension de 4ta sin complicaciones.',
          'S/ 14.90',
          const ['Recibos por honorarios', 'Suspension de 4ta anual', 'Tramite hecho por SUMAUP360', 'Historial de solicitudes', 'Alertas SUNAT'],
        ),
      _ => (
          'Comprobantes',
          'Genera tu QR, recibe solicitudes de boleta o factura de tus clientes y gestiona tus comprobantes.',
          'S/ 39.90',
          const ['Boletas y facturas ilimitadas', 'QR para tus clientes', 'Declaracion mensual', 'Codigo NPS', 'Historial de comprobantes'],
        ),
    };

    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.xl),
      children: [
        Text(title, style: AppText.h1),
        const SizedBox(height: AppSpacing.lg),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(AppSpacing.xl),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              begin: Alignment.topLeft, end: Alignment.bottomRight,
              colors: [_indigo, AppColors.primary],
            ),
            borderRadius: BorderRadius.circular(24),
            boxShadow: AppShadows.primary,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 44, height: 44,
                    decoration: BoxDecoration(color: _gold.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(12)),
                    alignment: Alignment.center,
                    child: const AppIcon('crown', size: 26, color: _gold),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Text('Funcion Premium', style: AppText.h2.copyWith(color: Colors.white)),
                ],
              ),
              const SizedBox(height: AppSpacing.lg),
              Text(desc, style: AppText.body.copyWith(color: Colors.white.withValues(alpha: 0.9))),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        ...bullets.map((b) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: Row(
                children: [
                  const AppIcon('check-circle', size: 18, color: AppColors.success),
                  const SizedBox(width: 8),
                  Expanded(child: Text(b, style: AppText.body)),
                ],
              ),
            )),
        const SizedBox(height: AppSpacing.lg),
        SizedBox(
          height: 54,
          child: ElevatedButton(
            onPressed: () => context.push(Routes.plans),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const AppIcon('crown', size: 20, color: Colors.white),
                const SizedBox(width: 8),
                Text('Activar Premium  $price', style: AppText.button),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
