package org.manolin.ftpblost.managers;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.manolin.ftpblost.logs.LogsManager;

public class CryptoManager {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final byte[] IV = new byte[16];

    public static String encryptText(String plainText, String encryptionKey) throws Exception {
        try {
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), SECRET_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            LogsManager.logDebug("Text encrypted successfully");
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            LogsManager.logError("Error encrypting text: " + e.getMessage(), e);
            throw e;
        }
    }

    public static String decryptText(String cipherTextBase64, String decryptionKey) throws Exception {
        try {
            SecretKey secretKey = new SecretKeySpec(decryptionKey.getBytes(StandardCharsets.UTF_8), SECRET_KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
            byte[] decodedBytes = Base64.getDecoder().decode(cipherTextBase64);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            LogsManager.logDebug("Text decrypted successfully");
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            LogsManager.logError("Error decrypting text: " + e.getMessage(), e);
            throw e;
        }
    }

    public static String generateAES(int length) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = new SecureRandom();
            keyGenerator.init(length, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            String key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            LogsManager.logInfo("AES key generated successfully with length: " + length);
            return key;
        } catch (NoSuchAlgorithmException e) {
            LogsManager.logError("Error generating AES key: " + e.getMessage(), e);
            return null;
        }
    }
}