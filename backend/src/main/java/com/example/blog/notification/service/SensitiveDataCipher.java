package com.example.blog.notification.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class SensitiveDataCipher {

    private static final int IV_LENGTH = 12;
    private final SecretKey encryptionKey;
    private final SecureRandom random = new SecureRandom();

    public SensitiveDataCipher(SecretKey secretKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secretKey.getEncoded());
            this.encryptionKey = new SecretKeySpec(digest, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot initialize sensitive data encryption", exception);
        }
    }

    public byte[] encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot encrypt sensitive data", exception);
        }
    }

    public String decrypt(byte[] value) {
        if (value == null || value.length <= IV_LENGTH) return null;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot decrypt sensitive data", exception);
        }
    }
}
