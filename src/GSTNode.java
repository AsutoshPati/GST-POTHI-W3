import java.util.List;

/**
 * A GSTNode simulates a validator (like a regional tax office) that
 * independently re-checks the whole chain. Extends NetworkParticipant
 * to get an id, name, and wallet the same way Business does.
 */
public class GSTNode extends NetworkParticipant {

    private int blocksValidated = 0;

    public GSTNode(String name) {
        super(name);
    }

    @Override
    public String getRole() {
        return "Validator";
    }

    public int getBlocksValidated() {
        return blocksValidated;
    }

    /** Re-validates a whole blockchain and counts it towards this node's tally. */
    public Blockchain.ValidationResult validate(Blockchain chain) {
        Blockchain.ValidationResult result = chain.validate();
        blocksValidated++;
        return result;
    }

    /** Demo: build a tiny valid chain and have this node validate it. */
    public static void main(String[] args) {
        GSTNode node = new GSTNode("Validator-Alpha");
        Blockchain chain = new Blockchain(3);

        Wallet a = new Wallet(), b = new Wallet();
        Invoice inv = new Invoice("INV/A/001", "A", a.getAddress(), "B", b.getAddress(),
                List.of(new LineItem("Cement Bags", 20, 400, 18)));
        inv.signAsSeller(a.getPublicKeyBase64(), a.getPrivateKeyBase64());
        inv.signAsBuyer(b.getPublicKeyBase64(), b.getPrivateKeyBase64());
        chain.addBlock(List.of(inv), node.getName());

        Blockchain.ValidationResult result = node.validate(chain);
        System.out.println(node);
        System.out.println("Validation result: " + result.message);
        System.out.println("Blocks validated so far: "
                + node.getBlocksValidated());
    }
}