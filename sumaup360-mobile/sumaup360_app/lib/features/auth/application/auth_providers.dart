import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../core/errors/app_exception.dart';

/// Instancia de FirebaseAuth.
final firebaseAuthProvider = Provider<FirebaseAuth>((ref) => FirebaseAuth.instance);

/// Estado de autenticacion (usuario Firebase o null).
final authStateProvider = StreamProvider<User?>((ref) {
  return ref.watch(firebaseAuthProvider).authStateChanges();
});

/// Controlador de acciones de autenticacion (Firebase es la identidad).
final authControllerProvider = Provider<AuthController>((ref) {
  return AuthController(ref.watch(firebaseAuthProvider));
});

class AuthController {
  AuthController(this._auth);
  final FirebaseAuth _auth;

  User? get currentUser => _auth.currentUser;
  bool get isEmailVerified => _auth.currentUser?.emailVerified ?? false;

  Future<void> signInWithEmail(String email, String password) async {
    try {
      await _auth.signInWithEmailAndPassword(email: email.trim(), password: password);
    } on FirebaseAuthException catch (e) {
      throw AppException(_msg(e));
    }
  }

  Future<void> registerWithEmail(String email, String password) async {
    try {
      final cred = await _auth.createUserWithEmailAndPassword(email: email.trim(), password: password);
      await cred.user?.sendEmailVerification();
    } on FirebaseAuthException catch (e) {
      throw AppException(_msg(e));
    }
  }

  Future<void> signInWithGoogle() async {
    try {
      final googleUser = await GoogleSignIn().signIn();
      if (googleUser == null) throw AppException('Inicio con Google cancelado.');
      final googleAuth = await googleUser.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );
      await _auth.signInWithCredential(credential);
    } on FirebaseAuthException catch (e) {
      throw AppException(_msg(e));
    }
  }

  Future<void> sendPasswordReset(String email) async {
    try {
      await _auth.sendPasswordResetEmail(email: email.trim());
    } on FirebaseAuthException catch (e) {
      throw AppException(_msg(e));
    }
  }

  Future<void> resendVerification() async => _auth.currentUser?.sendEmailVerification();

  Future<bool> reloadAndCheckVerified() async {
    await _auth.currentUser?.reload();
    return _auth.currentUser?.emailVerified ?? false;
  }

  Future<void> signOut() async => _auth.signOut();

  String _msg(FirebaseAuthException e) {
    switch (e.code) {
      case 'invalid-email':
        return 'Correo invalido.';
      case 'user-not-found':
      case 'wrong-password':
      case 'invalid-credential':
        return 'Correo o contrasena incorrectos.';
      case 'email-already-in-use':
        return 'Ese correo ya tiene una cuenta.';
      case 'weak-password':
        return 'La contrasena es muy debil (minimo 6 caracteres).';
      case 'network-request-failed':
        return 'Sin conexion. Revisa tu internet.';
      case 'too-many-requests':
        return 'Demasiados intentos. Intenta mas tarde.';
      default:
        return 'No se pudo completar. Intenta de nuevo.';
    }
  }
}
