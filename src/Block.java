import java.util.List;

/**
 * A Block bundles a batch of invoices and links to the previous block
 * by hash. Extends Hashable to reuse the SHA-256 hashing logic.
 * mine() is the proof-of-work step: it searches for a nonce that makes
 * the block's hash start with N zeros, the same idea Bitcoin mining uses.
 */
public class Block extends Hashable {

    private final int index;
    private final String previousHash;
    private final List<Invoice> invoices;
    private final String minedBy;
    private int nonce;
    private String hash;

    public Block(int index, String previousHash, List<Invoice> invoices, String minedBy) {
        super();
        this.index = index;
        this.previousHash = previousHash;
        this.invoices = invoices;
        this.minedBy = minedBy;
        this.nonce = 0;
        this.hash = computeHash();
    }

    /** Includes the nonce so every mining attempt produces a different hash. */
    @Override
    protected String getDataToHash() {
        StringBuilder invoiceHashes = new StringBuilder();
        for (Invoice inv : invoices) invoiceHashes.append(inv.computeHash());
        return index + previousHash + timestamp + nonce + invoiceHashes;
    }

    /** Proof-of-work: keep changing the nonce until the hash has enough leading zeros. */
    public void mine(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.startsWith(target)) {
            nonce++;
            hash = computeHash();
        }
    }

    public int getIndex() { return index; }
    public String getPreviousHash() { return previousHash; }
    public List<Invoice> getInvoices() { return invoices; }
    public String getMinedBy() { return minedBy; }
    public int getNonce() { return nonce; }
    public String getHash() { return hash; }

    /** Demo: build a block from two invoices and mine it, showing the nonce search. */
    public static void main(String[] args) {
        Wallet a = new Wallet(), b = new Wallet();
        Invoice inv1 = new Invoice("INV/A/001", "A", a.getAddress(), "B", b.getAddress(),
                List.of(new LineItem("Steel Rods", 5, 2000, 18)));
        inv1.signAsSeller(a.getPublicKeyBase64(), a.getPrivateKeyBase64());
        inv1.signAsBuyer(b.getPublicKeyBase64(), b.getPrivateKeyBase64());

        Block block = new Block(1, "0000000000", List.of(inv1), "Validator-Alpha");
        System.out.println("Hash before mining: " + block.getHash());

        long start = System.currentTimeMillis();
        block.mine(4);
        long ms = System.currentTimeMillis() - start;

        System.out.println("Hash after mining : " + block.getHash());
        System.out.println("Nonce found: " + block.getNonce() + " in " + ms + " ms");
    }
}
