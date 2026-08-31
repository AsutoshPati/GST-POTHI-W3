import java.util.ArrayList;
import java.util.List;

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

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public List<Block> getChain() {
        return chain;
    }

    public int getDifficulty() {
        return difficulty;
    }

    /** Mines a new block from a batch of invoices and appends it to the chain. */
    public Block addBlock(List<Invoice> invoices, String minedBy) {
        Block block = new Block(chain.size(), getLatestBlock().getHash(), invoices, minedBy);
        block.mine(difficulty);
        chain.add(block);
        return block;
    }

    /** Checks hash linkage, proof-of-work, and every invoice's signatures. */
    public ValidationResult validate() {
        String target = "0".repeat(difficulty);
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(current.computeHash()))
                return ValidationResult.fail("Block " + i + " hash does not match its data - tampering detected");
            if (!current.getPreviousHash().equals(previous.getHash()))
                return ValidationResult.fail("Block " + i + " is not linked to block " + (i - 1));
            if (!current.getHash().startsWith(target))
                return ValidationResult.fail("Block " + i + " fails proof-of-work difficulty");
            for (Invoice inv : current.getInvoices()) {
                if (!inv.verifySignatures())
                    return ValidationResult.fail("Block " + i + " has an invalid signature on invoice " + inv.getId());
            }
        }
        return ValidationResult.ok();
    }

    /** Simple pass/fail wrapper returned by validate(). */
    public static class ValidationResult {
        public final boolean valid;
        public final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "Chain is valid");
        }

        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
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
    }
}