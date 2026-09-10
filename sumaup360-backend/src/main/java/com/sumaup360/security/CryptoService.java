package com.sumaup360.security;

import com.sumaup360.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simetrico AES-256-GCM para datos sensibles (p. ej. Clave SOL).
 * La clave AES se deriva con SHA-256 del secreto configurado (security.encryption-key).
 * Formato de salida: base64( IV[12] || ciphertext+tag ).
 *
 * Nota: el secreto debe inyectarse por variable de entorno ENCRYPTION_KEY en prod.
 */
@Service
public class CryptoService {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${security.encryption-key:sumaup360-dev-encryption-change-me}") String secret) {
        try {
            this.key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el cifrado", e);
        }
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes("UTF-8"));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar", e);
        }
    }

    public String decrypt(String b64) {
        if (b64 == null) return null;
        try {
            byte[] in = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(in, 0, iv, 0, IV_LEN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(in, IV_LEN, in.length - IV_LEN);
            return new String(pt, "UTF-8");
        } catch (Exception e) {
            throw new BadRequestException("No se pudo descifrar la credencial.");
        }
    }
}
