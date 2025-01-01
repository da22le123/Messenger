package utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CheckSumCalculator {
    public static String calculateSHA256(String filePath) throws IOException, NoSuchAlgorithmException {

        // Create a MessageDigest instance for SHA-256
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

        // Read the file in a buffered way and update the digest
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, sha256Digest)) {

            // Read the entire file to update the digest
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // Just read, the DigestInputStream updates sha256Digest automatically
            }
        }

        // Once all the data is read, retrieve the digest’s bytes
        byte[] digestBytes = sha256Digest.digest();

        // Convert the byte array into a hexadecimal string
        return bytesToHex(digestBytes);
    }

    // Helper method to convert a byte array to a hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
