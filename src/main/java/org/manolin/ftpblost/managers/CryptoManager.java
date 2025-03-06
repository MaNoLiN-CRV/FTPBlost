package org.manolin.ftpblost.managers;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoManager {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String IV = "adhbsndkhj27ue87187163jhgdgu>{}@13214";

    public static String encryptText(String plainText, String encryptionKey) throws Exception {
        SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), SECRET_KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptText(String cipherTextBase64, String decryptionKey) throws Exception {
        SecretKey secretKey = new SecretKeySpec(decryptionKey.getBytes(StandardCharsets.UTF_8), SECRET_KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
        byte[] decodedBytes = Base64.getDecoder().decode(cipherTextBase64);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}