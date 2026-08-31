import java.util.UUID;

/**
 * Abstract base for anything that needs a tamper-evident fingerprint.
 * - A unique identifier
 * - A creation timestamp
 * - A tamper-evident SHA-256 fingerprint
 * Invoice and Block both extend this and only need to say what data
 * they want hashed - the actual SHA-256 logic lives here, once.
 */
public abstract class Hashable {

    /** Unique identifier for this object. */
    protected final UUID id;

    /** Time when this object was created. */
    protected final long timestamp;

    protected Hashable() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Subclasses must return the exact data that should be included
     * in the SHA-256 hash.
     * 
     * The returned string should be deterministic: the same
     * object state should always produce the same string.
     */
    protected abstract String getDataToHash();

    /**
     * Computes a SHA-256 hash of the data returned by getDataToHash().
     * The actual hashing logic lives in CommonUtils, shared by any
     * class that needs it - not just Hashable subclasses.
     *
     * @return SHA-256 hash represented as a hexadecimal string
     */
    public String computeHash() {
        return CommonUtils.sha256(getDataToHash());
    }

    /** Returns the unique ID of this object. */
    public UUID getId() {
        return id;
    }

    /** Returns the creation timestamp. */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Demo showing that even a tiny change in the data produces a completely
     * different SHA-256 hash.
     *
     * Note: Every Hashable also carries its own ID and timestamp; They are
     * deliberately left OUT of getDataToHash() here, so the amount is the only
     * thing that differs between the two objects below. That isolates exactly
     * one variable: changing the amount changes the hash - nothing else about
     * the two objects differs in what gets hashed.
     */
    public static void main(String[] args) {
        Hashable original = new Hashable() {
            @Override
            protected String getDataToHash() {
                String dataToHash = "invoice:INV-001|amount:5000";
                System.out.println("Data: " + dataToHash);
                return dataToHash;
            }
        };
        Hashable tampered = new Hashable() {
            @Override
            protected String getDataToHash() {
                String dataToHash = "invoice:INV-001|amount:5001";
                System.out.println("Data: " + dataToHash);
                return dataToHash;
            }
        };
        System.out.println("Original hash : " + original.computeHash());
        System.out.println();
        System.out.println("Tampered hash : " + tampered.computeHash());
        System.out.println();
    }
}
