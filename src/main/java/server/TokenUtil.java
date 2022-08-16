package server;

import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class TokenUtil {

    public static String encrypt(Token token, String keyAndIv) throws Exception {
        String algorithm = "AES/CBC/PKCS5Padding";
        String input = token.toString();
        SecretKey key = AESUtil.stringToKey(keyAndIv);
        IvParameterSpec iv = AESUtil.stringToIv(keyAndIv);
        return AESUtil.encrypt(algorithm, input, key, iv);
    }

    public static Token decrypt(String cipherText, String keyAndIv) throws Exception {
        String algorithm = "AES/CBC/PKCS5Padding";
        SecretKey key = AESUtil.stringToKey(keyAndIv);
        IvParameterSpec iv = AESUtil.stringToIv(keyAndIv);

        String decrypted = AESUtil.decrypt(algorithm, cipherText, key, iv);
        String[] tokenParts = decrypted.split("\\|");
        if (tokenParts.length != 6) {
            throw new Exception("Invalid token");
        }
        if (!tokenParts[0].equals("v1")) {
            throw new Exception("Invalid token version: " + tokenParts[0]);
        }
        return new Token(tokenParts[1], new Date(Long.parseLong(tokenParts[2])),
                new Date(Long.parseLong(tokenParts[3])), tokenParts[4]);

    }


}
