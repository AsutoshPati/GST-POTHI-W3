import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * A Wallet is a business's cryptographic identity: a keypair used to
 * sign invoices (private key) and let others verify them (public key).
 * The "address" is a short fingerprint of the public key.
 * Note: this is real crypto for real digital signatures, but no actual
 * cryptocurrency or money is involved anywhere in this project.
 */
public class Wallet {

    private final KeyPair keyPair;
    private final String address;

    public Wallet() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            this.keyPair = gen.generateKeyPair();
            // reuses CommonUtils.sha256() - shared, not tied to the Hashable class
            this.address = CommonUtils.sha256(getPublicKeyBase64()).substring(0, 20);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate wallet", e);
        }
    }

    public String getAddress() { return address; }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public String getPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /** Signs data with a private key, returns a Base64 signature. */
    public static String sign(String privateKeyBase64, String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey priv = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(priv);
            sig.update(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }

    /** Verifies a signature was really made by the holder of publicKeyBase64. */
    public static boolean verify(String publicKeyBase64, String data, String signatureBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey pub = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(pub);
            sig.update(data.getBytes("UTF-8"));
            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    /** Demo: create a wallet, sign a message, verify it, then show a forged signature failing. */
    public static void main(String[] args) {
        Wallet w = new Wallet();
        System.out.println("Address: " + w.getAddress());

        String message = "Invoice INV-001 for Rs.5000";
        String signature = sign(w.getPrivateKeyBase64(), message);
        System.out.println("Signature: " + signature.substring(0, 30) + "...");

        boolean genuine = verify(w.getPublicKeyBase64(), message, signature);
        System.out.println("Verify genuine signature: " + genuine);

        boolean forged = verify(w.getPublicKeyBase64(), "Invoice INV-001 for Rs.9999", signature);
        System.out.println("Verify against altered message: " + forged);
    }
}
