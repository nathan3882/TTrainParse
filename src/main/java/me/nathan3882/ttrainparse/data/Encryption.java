package me.nathan3882.ttrainparse.data;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Encryption {

    private String originalPassword;
    private byte[] salt;
    private byte[] originalEncrypted;

    public Encryption(byte[] originalEncrypted, byte[] salt) {
        this.salt = salt;
        this.originalEncrypted = originalEncrypted;
    }

    public Encryption(String password, byte[] salt) {
        this.originalPassword = password;
        this.salt = salt;
        try {
            this.originalEncrypted = encrypt(getOriginalPassword(), getSalt());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        }
    }

    public static boolean authenticate(String attemptedPassword, String encodedStoredPasswordEncrypted, String encodedStoredSalt) {
        byte[] encryptedAttemptedPassword;
        String encodedAndEncryptedAttempted = "";
        try {
            byte[] decodedSalt = Base64.getDecoder().decode(encodedStoredSalt);
            encryptedAttemptedPassword = encrypt(attemptedPassword, decodedSalt);
            encodedAndEncryptedAttempted = Base64.getEncoder().encodeToString(encryptedAttemptedPassword);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return encodedAndEncryptedAttempted.equals(encodedStoredPasswordEncrypted);

    }

    private static byte[] encrypt(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        int derivedKeyLength = 160; //160 bits

        int iterations = 10000;

        KeySpec specification = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength);

        SecretKeyFactory keyGenerator = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        SecretKey generatedSecretKey = keyGenerator.generateSecret(specification);
        return generatedSecretKey.getEncoded();
    }

    public static byte[] generateSalt() {
        // VERY important to use SecureRandom instead of just Random
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        return salt;
    }

    public byte[] getSalt() {
        return this.salt;
    }

    public void setSalt(byte[] originalSalt) {
        this.salt = originalSalt;
    }

    public boolean authenticateWith(byte[] anotherEncryption, byte[] anotherSalt) {
        // Encrypts password using same salt that originalPassword pw used
        return Arrays.equals(anotherEncryption, getOriginalEncrypted());
    }

    public byte[] getOriginalEncrypted() {
        return this.originalEncrypted;
    }

    private String getOriginalPassword() {
        return this.originalPassword;
    }
}