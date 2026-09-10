/// Perfil de usuario (mapea ProfileMeResponse del backend).
class ProfileMe {
  ProfileMe({
    this.fullName,
    this.email,
    this.phone,
    this.photoUrl,
    this.countryCode,
    this.referralCode,
    this.profileCompleted = false,
    this.segmentCode,
    this.ruc,
    this.regime,
    this.clientType,
    this.dni,
    this.firstName,
    this.lastName,
    this.onboardingCompleted = false,
    this.orientationStatus,
  });

  final String? fullName;
  final String? email;
  final String? phone;
  final String? photoUrl;
  final String? countryCode;
  final String? referralCode;
  final bool profileCompleted;
  final String? segmentCode;
  final String? ruc;
  final String? regime;
  final String? clientType; // TAXISTA | DELIVERY_PEYA | SERVICIOS_PROFESIONALES
  final String? dni;
  final String? firstName;
  final String? lastName;
  final bool onboardingCompleted;
  final String? orientationStatus; // PENDING | COMPLETED | NOT_REQUIRED | null

  bool get orientationPending => orientationStatus == 'PENDING';

  bool get isTaxista => clientType == 'TAXISTA';
  bool get isDeliveryPeya => clientType == 'DELIVERY_PEYA';
  bool get isServiciosProfesionales => clientType == 'SERVICIOS_PROFESIONALES';

  factory ProfileMe.fromJson(Map<String, dynamic> j) => ProfileMe(
        fullName: j['fullName'] as String?,
        email: j['email'] as String?,
        phone: j['phone'] as String?,
        photoUrl: j['photoUrl'] as String?,
        countryCode: j['countryCode'] as String?,
        referralCode: j['referralCode'] as String?,
        profileCompleted: (j['profileCompleted'] as bool?) ?? false,
        segmentCode: j['segmentCode'] as String?,
        ruc: j['ruc'] as String?,
        regime: j['regime'] as String?,
        clientType: j['clientType'] as String?,
        dni: j['dni'] as String?,
        firstName: j['firstName'] as String?,
        lastName: j['lastName'] as String?,
        onboardingCompleted: (j['onboardingCompleted'] as bool?) ?? false,
        orientationStatus: j['orientationStatus'] as String?,
      );
}
