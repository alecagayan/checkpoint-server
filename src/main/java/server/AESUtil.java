package server;

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

public class AESUtil {

    public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static String generateKeyAndIv(int n) throws NoSuchAlgorithmException {
        SecretKey key = generateKey(n);
        IvParameterSpec iv = generateIv();
        return keyAndIvToString(key, iv);
    }

    public static String encrypt(String algorithm, String input, SecretKey key,
            IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }

    public static String decrypt(String algorithm, String cipherText, SecretKey key,
            IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
        return new String(plainText);
    }

    public static String keyAndIvToString(SecretKey key, IvParameterSpec iv) {
        return Base64.getEncoder()
                .encodeToString(key.getEncoded()) + ":"
                + Base64.getEncoder()
                        .encodeToString(iv.getIV());
    }

    public static SecretKey stringToKey(String keyAndIvString) {
        String[] keyAndIv = keyAndIvString.split(":");
        byte[] keyBytes = Base64.getDecoder()
                .decode(keyAndIv[0]);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    public static IvParameterSpec stringToIv(String keyAndIvString) {
        String[] keyAndIv = keyAndIvString.split(":");
        byte[] ivBytes = Base64.getDecoder()
                .decode(keyAndIv[1]);
        return new IvParameterSpec(ivBytes);
    }

}
