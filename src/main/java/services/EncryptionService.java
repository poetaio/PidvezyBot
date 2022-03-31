package services;

import models.hibernate.User;
import org.hibernate.Session;
import services.utils.Constants;
import utils.HibernateUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionService {
    public static String encrypt(String input) throws RuntimeException {
        if (input == null)
            return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] keyBytes = System.getenv(Constants.ENCRYPTION_SECRET).getBytes();
            Key key = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
            Arrays.fill(keyBytes, (byte) 0);
            byte[] cipherText = cipher.doFinal(input.getBytes());
            return Base64.getEncoder()
                    .encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Could not encrypt string");
        }
    }

    public static String decrypt(String input) throws RuntimeException {
        if (input == null)
            return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] keyBytes = System.getenv(Constants.ENCRYPTION_SECRET).getBytes();
            Key key = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));
            Arrays.fill(keyBytes, (byte) 0);
            byte[] plainText = cipher.doFinal(Base64.getDecoder()
                    .decode(input));
            return new String(plainText);
        } catch (Exception e) {
            throw new RuntimeException("Could not encrypt string");
        }
    }
}

