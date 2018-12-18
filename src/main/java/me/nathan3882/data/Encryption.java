package me.nathan3882.data;

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

    public byte[] getSalt() {
        return this.salt;
    }

    private String getOriginalPassword() {
        return this.originalPassword;
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

    public boolean authenticateWith(byte[] anotherEncryption, byte[] anotherSalt) {
        // Encrypts password using same salt that originalPassword pw used
        return Arrays.equals(anotherEncryption, getOriginalEncrypted());
    }

    public byte[] getOriginalEncrypted() {
        return this.originalEncrypted;
    }

    private static byte[] encrypt(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // PBKDF2 with SHA-1 as the hashing algorithm. Note that the NIST
        // specifically names SHA-1 as an acceptable hashing algorithm for PBKDF2

        String algorithm = "PBKDF2WithHmacSHA1";

        // SHA-1 generates 160 bit hashes, so that's what makes sense here
        int derivedKeyLength = 160;

        // The NIST recommends at least 1,000 iterations:
        // iOS 4.x reportedly uses 10,000:
        int iterations = 10000;

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength);

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);

        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
        byte[] encrypted = secretKey.getEncoded();
        return encrypted;
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

    public void setSalt(byte[] originalSalt) {
        this.salt = originalSalt;
    }
}