import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_spacing.dart';
import '../../../core/constants/app_text_styles.dart';
import '../../../shared/widgets/app_icon.dart';
import '../../profile/application/profile_providers.dart';
import '../domain/plan_catalog.dart';

const _gold = Color(0xFFF5C046);
const _indigo = Color(0xFF161A40);

/// Pantalla de plan recomendado (SUMAUP PRO), derivado del diagnostico.
class PlanScreen extends ConsumerStatefulWidget {
  const PlanScreen({super.key});

  @override
  ConsumerState<PlanScreen> createState() => _PlanScreenState();
}

class _PlanScreenState extends ConsumerState<PlanScreen> {
  bool _annual = true;

  String _perMonth(double monthly) {
    final v = _annual ? monthly * 10 / 12 : monthly;
    return 'S/ ${v.toStringAsFixed(2)}';
  }

  @override
  Widget build(BuildContext context) {
    final profileAsync = ref.watch(profileMeProvider);
    final profile = profileAsync.valueOrNull;
    final segment = profile?.segmentCode;
    final plan = planForSegment(segment);

    final loading = profileAsync.isLoading && segment == null;

    return Scaffold(
      backgroundColor: AppColors.surfaceAlt,
      appBar: AppBar(
        backgroundColor: AppColors.surfaceAlt,
        title: const Text('Tu plan'),
      ),
      body: SafeArea(
        child: loading
            ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
            : plan == null
                ? const _NoDiagnosisView()
                : _RecommendedView(plan: plan, perMonth: _perMonth, annual: _annual, onToggle: (v) => setState(() => _annual = v)),
      ),
      bottomNavigationBar: (loading || plan == null)
          ? null
          : _CtaBar(
              label: 'Activar ${plan.name}',
              price: '${_perMonth(plan.monthly)}/mes',
              onTap: () => showModalBottomSheet<void>(
                context: context,
                backgroundColor: AppColors.surface,
                shape: const RoundedRectangleBorder(
                  borderRadius: BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
                ),
                builder: (_) => const _ComingSoonSheet(),
              ),
            ),
    );
  }
}

/// Vista cuando el usuario aun no tiene diagnostico (o segmento no mapeado).
class _NoDiagnosisView extends StatelessWidget {
  const _NoDiagnosisView();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.xl),
      children: [
        Container(
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
                width: 44,
                height: 44,
                decoration: BoxDecoration(color: _gold.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(12)),
                alignment: Alignment.center,
                child: const AppIcon('crown', size: 26, color: _gold),
              ),
              const SizedBox(height: AppSpacing.lg),
              Text('Descubre tu plan ideal', style: AppText.h2.copyWith(color: Colors.white)),
              const SizedBox(height: AppSpacing.sm),
              Text(
                'Responde un diagnostico corto y te recomendamos el plan hecho para tu actividad.',
                style: AppText.body.copyWith(color: Colors.white.withValues(alpha: 0.9)),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.xl),
        SizedBox(
          height: 54,
          child: ElevatedButton(
            onPressed: () => context.push(Routes.aiDiagnosis),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                AppIcon('circle-dashed-percentage', size: 20, color: Colors.white),
                SizedBox(width: 8),
                Text('Hacer mi diagnostico', style: AppText.button),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Vista del plan recomendado segun el segmento.
class _RecommendedView extends StatelessWidget {
  const _RecommendedView({
    required this.plan,
    required this.perMonth,
    required this.annual,
    required this.onToggle,
  });

  final SegmentPlan plan;
  final String Function(double) perMonth;
  final bool annual;
  final ValueChanged<bool> onToggle;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.xl),
      children: [
        _Hero(plan: plan),
        const SizedBox(height: AppSpacing.xl),
        _BillingToggle(annual: annual, onChanged: onToggle),
        const SizedBox(height: AppSpacing.lg),
        _PriceCard(plan: plan, price: perMonth(plan.monthly), annual: annual),
        const SizedBox(height: AppSpacing.md),
        _Benefits(plan: plan),
        const SizedBox(height: AppSpacing.lg),
        const Row(
          children: [
            AppIcon('exclamation-circle', size: 16, color: AppColors.muted),
            SizedBox(width: 6),
            Expanded(
              child: Text(
                'Precio referencial recomendado por tu diagnostico. La activacion de planes estara disponible pronto.',
                style: AppText.small,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero({required this.plan});
  final SegmentPlan plan;

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
          Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(color: _gold.withValues(alpha: 0.2), borderRadius: BorderRadius.circular(12)),
                alignment: Alignment.center,
                child: const AppIcon('crown', size: 26, color: _gold),
              ),
              const SizedBox(width: AppSpacing.md),
              Text('Tu plan recomendado', style: AppText.small.copyWith(color: Colors.white.withValues(alpha: 0.85))),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),
          Text(plan.name, style: AppText.h1.copyWith(color: Colors.white)),
          const SizedBox(height: 4),
          Text(plan.tagline, style: AppText.body.copyWith(color: Colors.white.withValues(alpha: 0.9))),
          const SizedBox(height: AppSpacing.lg),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const AppIcon('circle-dashed-percentage', size: 16, color: Colors.white),
                const SizedBox(width: 6),
                Text(
                  'Segun tu diagnostico: ${segmentLabel(plan.segment)}',
                  style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BillingToggle extends StatelessWidget {
  const _BillingToggle({required this.annual, required this.onChanged});
  final bool annual;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusPill),
        boxShadow: AppShadows.card,
      ),
      child: Row(
        children: [
          _seg('Mensual', !annual, () => onChanged(false)),
          _seg('Anual  -17%', annual, () => onChanged(true)),
        ],
      ),
    );
  }

  Widget _seg(String label, bool active, VoidCallback onTap) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 10),
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

class _PriceCard extends StatelessWidget {
  const _PriceCard({required this.plan, required this.price, required this.annual});
  final SegmentPlan plan;
  final String price;
  final bool annual;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        border: Border.all(color: AppColors.primary, width: 2),
        boxShadow: AppShadows.card,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(plan.name, style: AppText.bodyStrong),
                const SizedBox(height: 2),
                Text(
                  annual ? 'Facturado anual (S/ ${(plan.monthly * 10).toStringAsFixed(2)}/ano)' : 'Facturado mensual',
                  style: AppText.small,
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(price, style: AppText.h2.copyWith(color: AppColors.primary)),
              const Text('/mes', style: AppText.small),
            ],
          ),
        ],
      ),
    );
  }
}

class _Benefits extends StatelessWidget {
  const _Benefits({required this.plan});
  final SegmentPlan plan;

  @override
  Widget build(BuildContext context) {
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
          const Text('Que incluye', style: AppText.title),
          const SizedBox(height: AppSpacing.md),
          ...plan.benefits.map((b) => Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: Row(
                  children: [
                    Container(
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(color: AppColors.primarySoft, borderRadius: BorderRadius.circular(10)),
                      alignment: Alignment.center,
                      child: AppIcon(b.icon, size: 19, color: AppColors.primary),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(child: Text(b.title, style: AppText.body)),
                    const AppIcon('check-circle', size: 18, color: AppColors.success),
                  ],
                ),
              )),
        ],
      ),
    );
  }
}

class _CtaBar extends StatelessWidget {
  const _CtaBar({required this.label, required this.price, required this.onTap});
  final String label;
  final String price;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(AppSpacing.lg, AppSpacing.md, AppSpacing.lg, AppSpacing.lg),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        boxShadow: AppShadows.floating,
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 54,
          child: ElevatedButton(
            onPressed: onTap,
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
                Flexible(child: Text(label, style: AppText.button, overflow: TextOverflow.ellipsis)),
                const SizedBox(width: 8),
                Text('· $price', style: AppText.button.copyWith(color: Colors.white.withValues(alpha: 0.85))),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ComingSoonSheet extends StatelessWidget {
  const _ComingSoonSheet();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(AppSpacing.xl, AppSpacing.lg, AppSpacing.xl, AppSpacing.xl),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(color: AppColors.border, borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(color: _gold.withValues(alpha: 0.18), borderRadius: BorderRadius.circular(16)),
            alignment: Alignment.center,
            child: const AppIcon('crown', size: 30, color: Color(0xFFB57E12)),
          ),
          const SizedBox(height: AppSpacing.md),
          const Text('Activacion de planes', style: AppText.title),
          const SizedBox(height: AppSpacing.sm),
          const Text(
            'Muy pronto podras activar tu plan y desbloquear todas las funciones. Te avisaremos cuando este listo.',
            textAlign: TextAlign.center,
            style: AppText.body,
          ),
          const SizedBox(height: AppSpacing.lg),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppSpacing.radius)),
              ),
              child: const Text('Entendido', style: AppText.button),
            ),
          ),
        ],
      ),
    );
  }
}
