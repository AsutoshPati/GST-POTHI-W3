import java.util.*;

/**
 * The ledger: an ordered list of Blocks, linked by hash.
 * validate() is what lets any node independently confirm the whole
 * history is intact, without trusting a central authority.
 */
public class Blockchain {

    private final List<Block> chain = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        this.difficulty = difficulty;
        Block genesis = new Block(0, "0", new ArrayList<>(), "GENESIS");
        genesis.mine(difficulty);
        chain.add(genesis);
    }

    public Block getLatestBlock() { return chain.get(chain.size() - 1); }
    public List<Block> getChain() { return chain; }
    public int getDifficulty() { return difficulty; }

    /** Mines a new block from a batch of invoices and appends it to the chain. */
    public Block addBlock(List<Invoice> invoices, String minedBy) {
        Block block = new Block(chain.size(), getLatestBlock().getHash(), invoices, minedBy);
        block.mine(difficulty);
        chain.add(block);
        return block;
    }

    /**
     * Checks every block individually - hash integrity, proof-of-work, link
     * to the previous block, and every invoice's signatures - and returns
     * a full report for EVERY block, not just the first failure. Used by
     * the tree visualization so each block can be colored on its own merit.
     */
    public List<BlockCheck> validateAll() {
        String target = "0".repeat(difficulty);
        List<BlockCheck> results = new ArrayList<>();

        for (int i = 0; i < chain.size(); i++) {
            Block current = chain.get(i);
            boolean hashOk = current.getHash().equals(current.computeHash());
            boolean linkOk = (i == 0) || current.getPreviousHash().equals(chain.get(i - 1).getHash());
            boolean powOk = current.getHash().startsWith(target);

            boolean signaturesOk = true;
            String badInvoiceNote = null;
            for (Invoice inv : current.getInvoices()) {
                if (!inv.verifySignatures()) {
                    signaturesOk = false;
                    badInvoiceNote = "invalid signature on invoice " + inv.getId();
                    break;
                }
            }
            results.add(new BlockCheck(i, hashOk, linkOk, powOk, signaturesOk, badInvoiceNote));
        }
        return results;
    }

    /**
     * Checks hash linkage, proof-of-work, and every invoice's signatures.
     * Blockchain trust is sequential: once one block fails, every block
     * after it is unverifiable too, even if it looks fine on its own -
     * so this reports the FIRST broken block, and that verdict does not
     * change just because more (otherwise valid) blocks get added later.
     */
    public ValidationResult validate() {
        for (BlockCheck check : validateAll()) {
            if (!check.isValid()) {
                return ValidationResult.fail("Chain is broken starting at block " + check.index + " ("
                        + check.reason() + "). Every block after this point is also unverifiable, "
                        + "even if it checks out on its own - a real blockchain cannot heal a broken past block.");
            }
        }
        return ValidationResult.ok();
    }

    /** Per-block validation detail: which specific check(s) failed, if any. */
    public static class BlockCheck {
        public final int index;
        public final boolean hashOk, linkOk, powOk, signaturesOk;
        public final String badInvoiceNote;

        BlockCheck(int index, boolean hashOk, boolean linkOk, boolean powOk, boolean signaturesOk, String badInvoiceNote) {
            this.index = index; this.hashOk = hashOk; this.linkOk = linkOk;
            this.powOk = powOk; this.signaturesOk = signaturesOk; this.badInvoiceNote = badInvoiceNote;
        }

        public boolean isValid() { return hashOk && linkOk && powOk && signaturesOk; }

        public String reason() {
            if (!hashOk) return "hash does not match its data - tampering detected";
            if (!linkOk) return "not linked to the previous block";
            if (!powOk) return "fails proof-of-work difficulty";
            if (!signaturesOk) return badInvoiceNote;
            return "valid";
        }
    }

    /** Simple pass/fail wrapper returned by validate(). */
    public static class ValidationResult {
        public final boolean valid;
        public final String message;
        private ValidationResult(boolean valid, String message) { this.valid = valid; this.message = message; }
        public static ValidationResult ok() { return new ValidationResult(true, "Chain is valid"); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }

    /** Demo: build a 3-block chain of signed invoices and validate it. */
    public static void main(String[] args) {
        Blockchain chain = new Blockchain(3);
        Wallet a = new Wallet(), b = new Wallet(), c = new Wallet();

        Invoice inv1 = new Invoice("INV/A/001", "A", a.getAddress(), "B", b.getAddress(),
                List.of(new LineItem("Paint Cans", 30, 200, 18)));
        inv1.signAsSeller(a.getPublicKeyBase64(), a.getPrivateKeyBase64());
        inv1.signAsBuyer(b.getPublicKeyBase64(), b.getPrivateKeyBase64());
        chain.addBlock(List.of(inv1), "Validator-Alpha");

        Invoice inv2 = new Invoice("INV/B/007", "B", b.getAddress(), "C", c.getAddress(),
                List.of(new LineItem("Paint Cans (resale)", 30, 217, 18)));
        inv2.signAsSeller(b.getPublicKeyBase64(), b.getPrivateKeyBase64());
        inv2.signAsBuyer(c.getPublicKeyBase64(), c.getPrivateKeyBase64());
        chain.addBlock(List.of(inv2), "Validator-Beta");

        System.out.println("Chain length: " + chain.getChain().size());
        System.out.println("Validation: " + chain.validate().message);

        System.out.println();
        System.out.println("Per-block detail from validateAll():");
        for (Blockchain.BlockCheck check : chain.validateAll()) {
            System.out.println("  Block " + check.index + ": " + (check.isValid() ? "OK" : "BROKEN - " + check.reason()));
        }
    }
}
