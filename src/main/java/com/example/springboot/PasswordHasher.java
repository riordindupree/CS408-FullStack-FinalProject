package com.example.springboot;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int ITERATIONS = 210_000;
  private static final int KEY_LENGTH_BITS = 256;
  private static final int SALT_LENGTH_BYTES = 16;
  private static final SecureRandom RANDOM = new SecureRandom();

  public String hash(String password) {
    byte[] salt = new byte[SALT_LENGTH_BYTES];
    RANDOM.nextBytes(salt);
    return encode(password, salt, ITERATIONS);
  }

  public boolean matches(String password, String encodedPassword) {
    String[] parts = encodedPassword.split("\\$", -1);
    if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
      return false;
    }

    try {
      int iterations = Integer.parseInt(parts[1]);
      if (iterations < 1) {
        return false;
      }
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      byte[] actual = derive(password, salt, iterations);
      return MessageDigest.isEqual(expected, actual);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static String encode(String password, byte[] salt, int iterations) {
    byte[] derived = derive(password, salt, iterations);
    return "pbkdf2-sha256$" + iterations + "$"
        + Base64.getEncoder().encodeToString(salt) + "$"
        + Base64.getEncoder().encodeToString(derived);
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    char[] passwordChars = password.toCharArray();
    PBEKeySpec keySpec = new PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to securely hash the password.", exception);
    } finally {
      keySpec.clearPassword();
      Arrays.fill(passwordChars, '\0');
    }
  }
}
