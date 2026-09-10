import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_spacing.dart';
import '../../auth/presentation/widgets/auth_decor.dart';
import 'widgets/register_decor.dart';

/// Permiso de notificaciones push (registro-4.png). Sea cual sea la respuesta
/// del sistema, el flujo continua a la seleccion de actividad.
class RegisterNotificationsScreen extends StatefulWidget {
  const RegisterNotificationsScreen({super.key});

  @override
  State<RegisterNotificationsScreen> createState() => _RegisterNotificationsScreenState();
}

class _RegisterNotificationsScreenState extends State<RegisterNotificationsScreen> {
  bool _loading = false;

  Future<void> _activate() async {
    setState(() => _loading = true);
    try {
      // Muestra el dialogo del sistema (Android 13+ / iOS). La decision del
      // usuario queda registrada en el sistema; no bloquea el flujo.
      await FirebaseMessaging.instance.requestPermission();
    } catch (_) {
      // Sin permiso o sin Google Play Services: continuar igual.
    }
    if (!mounted) return;
    setState(() => _loading = false);
    context.push(Routes.registerActivity);
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SingleChildScrollView(
          child: Column(
            children: [
              RegisterHeader(onBack: () => context.backOr(Routes.registerPhone)),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  AppSpacing.xl,
                  AppSpacing.xxl,
                  AppSpacing.xl,
                  AppSpacing.xl + MediaQuery.paddingOf(context).bottom,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Center(child: AuthIconCircle(asset: AppAssets.icCampana)),
                    const SizedBox(height: AppSpacing.xxl),
                    const Text(
                      'Mantente al día',
                      textAlign: TextAlign.center,
                      style: RegisterText.titleCentered,
                    ),
                    const SizedBox(height: AppSpacing.md),
                    const Text(
                      'Recibe avisos sobre vencimientos, comprobantes procesados y novedades importantes de tu actividad.',
                      textAlign: TextAlign.center,
                      style: RegisterText.body,
                    ),
                    const SizedBox(height: AppSpacing.xxl),
                    AuthPrimaryButton(label: 'Activar notificaciones', loading: _loading, onPressed: _activate),
                    const SizedBox(height: AppSpacing.md),
                    RegisterTextButton(
                      label: 'Ahora no',
                      onPressed: () => context.push(Routes.registerActivity),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
