package me.simon76800.yrdsbstudentplanner.client;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Manages RSA key pair used to facilitate secure communication with the socket server.
 * Also provides key generation!
 */
public final class KeyHandler {
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    /**
     * Generates RSA key pair
     *
     * @return public key encoded into byte array
     * @throws NoSuchAlgorithmException if key pair algorithm does not exist
     */
    public static void generateKeys() throws NoSuchAlgorithmException {
        if(publicKey == null || privateKey == null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();
        }
    }

    /**
     * Decrypts given byte array message with local private key
     *
     * @param in message to decrypt
     * @return decrypted message, in byte array form
     */
    public static byte[] decryptRSA(byte[] in) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(in);
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }
}
