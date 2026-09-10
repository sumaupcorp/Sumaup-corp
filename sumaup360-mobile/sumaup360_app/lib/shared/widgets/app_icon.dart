import 'package:flutter/widgets.dart';
import 'package:hugeicons/hugeicons.dart';

import '../../core/constants/app_colors.dart';

/// Renderiza un icono por nombre semantico usando el set Hugeicons.
///
/// API estable (`AppIcon('bell', size: 22, color: ...)`): los nombres se mapean
/// aqui a iconos Hugeicons. Si un nombre no esta mapeado, cae a uno neutro.
class AppIcon extends StatelessWidget {
  const AppIcon(this.name, {super.key, this.size = 24, this.color, this.strokeWidth = 2.0});

  final String name;
  final double size;
  final Color? color;

  /// Grosor del trazo (hugeicons es stroke). Por defecto 2.0 para un look mas
  /// solido/bold (el default del paquete ~1.5 se ve delgado).
  final double strokeWidth;

  static const _fallback = HugeIcons.strokeRoundedInformationCircle;

  static const Map<String, dynamic> _map = {
    // Navegacion / estructura
    'home': HugeIcons.strokeRoundedHome01,
    'trending-up': HugeIcons.strokeRoundedAnalyticsUp,
    'trending-down': HugeIcons.strokeRoundedAnalyticsDown,
    'receipt': HugeIcons.strokeRoundedReceiptDollar,
    'user': HugeIcons.strokeRoundedUser,
    // Flechas / chevrons
    'arrow-bar-to-up': HugeIcons.strokeRoundedArrowUp01,
    'arrow-bar-to-down': HugeIcons.strokeRoundedArrowDown01,
    'square-rounded-arrow-left': HugeIcons.strokeRoundedArrowLeft01,
    'square-rounded-arrow-right': HugeIcons.strokeRoundedArrowRight01,
    'chevron-right': HugeIcons.strokeRoundedArrowRight01,
    'chevron-down': HugeIcons.strokeRoundedArrowDown01,
    // Alertas / notificaciones
    'bell': HugeIcons.strokeRoundedNotification01,
    'bell-check': HugeIcons.strokeRoundedNotification01,
    'bell-ringing': HugeIcons.strokeRoundedNotification03,
    'bell-x': HugeIcons.strokeRoundedNotification01,
    // Datos / analitica / impuestos
    'brand-google-analytics': HugeIcons.strokeRoundedAnalytics01,
    'circle-dashed-percentage': HugeIcons.strokeRoundedPercentCircle,
    // Calendario
    'calendar-event': HugeIcons.strokeRoundedCalendar01,
    'calendar-search': HugeIcons.strokeRoundedCalendar01,
    'calendar-week': HugeIcons.strokeRoundedCalendar03,
    // Plan / premium
    'crown': HugeIcons.strokeRoundedCrown,
    'crown-off': HugeIcons.strokeRoundedCrown,
    // Archivos / carpetas / export
    'download': HugeIcons.strokeRoundedDownload01,
    'file-download': HugeIcons.strokeRoundedFileDownload,
    'file-import': HugeIcons.strokeRoundedFileImport,
    'folder-minus': HugeIcons.strokeRoundedFolder01,
    'folder-off': HugeIcons.strokeRoundedFolder01,
    'folder-share': HugeIcons.strokeRoundedFolderShared01,
    'new-section': HugeIcons.strokeRoundedAddSquare,
    'square-plus': HugeIcons.strokeRoundedAddSquare,
    'square-rounded-plus': HugeIcons.strokeRoundedPlusSignCircle,
    // Edicion / vista
    'edit': HugeIcons.strokeRoundedEdit02,
    'eye': HugeIcons.strokeRoundedView,
    'eye-off': HugeIcons.strokeRoundedViewOff,
    'filter': HugeIcons.strokeRoundedFilter,
    'search': HugeIcons.strokeRoundedSearch01,
    // Estados / ayuda / tiempo
    'exclamation-circle': HugeIcons.strokeRoundedAlertCircle,
    'help-small': HugeIcons.strokeRoundedHelpCircle,
    'history-toggle': HugeIcons.strokeRoundedClock01,
    'flame': HugeIcons.strokeRoundedFire,
    'ghost-off': HugeIcons.strokeRoundedCancelCircle,
    'check': HugeIcons.strokeRoundedTick02,
    'check-circle': HugeIcons.strokeRoundedCheckmarkCircle02,
    // Correo
    'mail': HugeIcons.strokeRoundedMail01,
    'mail-exclamation': HugeIcons.strokeRoundedMail01,
    // Dinero
    'moneda-sol-peruano': HugeIcons.strokeRoundedMoney01,
    'moneybag': HugeIcons.strokeRoundedMoneyBag02,
    'moneybag-minus': HugeIcons.strokeRoundedMoneyBag01,
    'pig-money': HugeIcons.strokeRoundedPiggyBank,
    'receipt-tax': HugeIcons.strokeRoundedReceiptDollar,
    'coins': HugeIcons.strokeRoundedCoins01,
    // Foto / camara
    'photo': HugeIcons.strokeRoundedImage01,
    'photo-edit': HugeIcons.strokeRoundedImage02,
    'photo-square-rounded': HugeIcons.strokeRoundedImage01,
    'camera': HugeIcons.strokeRoundedCamera01,
    // Usuarios
    'user-circle': HugeIcons.strokeRoundedUserCircle,
    'user-edit': HugeIcons.strokeRoundedUserEdit01,
    'user-square-rounded': HugeIcons.strokeRoundedUser,
    'users': HugeIcons.strokeRoundedUserMultiple,
    // Formularios
    'lock': HugeIcons.strokeRoundedSquareLock01,
    'phone': HugeIcons.strokeRoundedSmartPhone01,
    'gift': HugeIcons.strokeRoundedGift,
    'id': HugeIcons.strokeRoundedUser,
    // Sistema
    'settings': HugeIcons.strokeRoundedSettings01,
    'logout': HugeIcons.strokeRoundedLogout01,
    // Formularios extra
    'notes': HugeIcons.strokeRoundedNote01,
    'tag': HugeIcons.strokeRoundedTags,
    'store': HugeIcons.strokeRoundedStore01,
    'dollar': HugeIcons.strokeRoundedDollarCircle,
    'plus': HugeIcons.strokeRoundedPlusSign,
    'circle': HugeIcons.strokeRoundedCircle,
    'magic': HugeIcons.strokeRoundedAiMagic,
    'image-upload': HugeIcons.strokeRoundedImageUpload01,
    'camera-add': HugeIcons.strokeRoundedCameraAdd01,
  };

  @override
  Widget build(BuildContext context) {
    return HugeIcon(
      icon: _map[name] ?? _fallback,
      size: size,
      color: color ?? AppColors.ink,
      strokeWidth: strokeWidth,
    );
  }
}
