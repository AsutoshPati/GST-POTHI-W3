import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * General-purpose helper methods shared across the project.
 * Anything that isn't specific to one class's job (hashing, formatting,
 * id generation, etc.) belongs here instead of being duplicated or
 * bolted onto an unrelated class.
 */
public final class CommonUtils {

    // Utility class - no instances needed.
    private CommonUtils() {
    }

    /**
     * Computes the SHA-256 hash of a string.
     * Used by Hashable (for tamper-evident fingerprints) and by Wallet
     * (to turn a public key into a short address) - one shared method,
     * no duplicated hashing logic.
     *
     * @param input the text to hash
     * @return SHA-256 hash represented as a hexadecimal string
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /** Demo: hash a sample string and show the result. */
    public static void main(String[] args) {
        String data = "GST POTHI W3";
        System.out.println("Input : " + data);
        System.out.println("SHA-256: " + sha256(data));
    }
}
