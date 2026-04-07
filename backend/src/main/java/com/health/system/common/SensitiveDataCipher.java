package com.health.system.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SensitiveDataCipher {

    private static final String PREFIX = "ENC:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BIT_LENGTH = 128;

    private final byte[] aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveDataCipher(@Value("${security.data.aes-key:}") String aesKey) {
        this.aesKey = normalizeKey(aesKey);
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText) || aesKey.length == 0 || plainText.startsWith(PREFIX)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_BIT_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new BusinessException("敏感数据加密失败");
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText) || aesKey.length == 0 || !cipherText.startsWith(PREFIX)) {
            return cipherText;
        }
        try {
            String payloadText = cipherText.substring(PREFIX.length());
            byte[] payload = Base64.getDecoder().decode(payloadText);
            if (payload.length <= IV_LENGTH) {
                return cipherText;
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_BIT_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException ex) {
            return cipherText;
        }
    }

    private byte[] normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return new byte[0];
        }
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        int targetLength;
        if (bytes.length >= 32) {
            targetLength = 32;
        } else if (bytes.length >= 24) {
            targetLength = 24;
        } else if (bytes.length >= 16) {
            targetLength = 16;
        } else {
            return new byte[0];
        }

        byte[] normalized = new byte[targetLength];
        System.arraycopy(bytes, 0, normalized, 0, targetLength);
        return normalized;
    }
}
