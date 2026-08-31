import java.util.HashMap;
import java.util.Map;

/**
 * Tracks how much Input Tax Credit each business has claimed, and only
 * allows a claim if a real, mined, fully-signed invoice backs it up.
 * This is the direct fix for fake ITC claims in real GST fraud.
 */
public class ITCLedger {

    private final Map<String, Double> claimedByBusiness = new HashMap<>();

    /**
     * Allows an ITC claim only if the invoice is genuine and actually appears in a
     * mined block.
     */
    public boolean claimITC(String businessId, Invoice invoice, Blockchain chain) {
        if (!invoice.verifySignatures())
            return false;
        if (!invoiceExistsInChain(invoice, chain))
            return false;

        claimedByBusiness.merge(businessId, invoice.getTotalGstAmount(), Double::sum);
        return true;
    }

    private boolean invoiceExistsInChain(Invoice invoice, Blockchain chain) {
        for (Block block : chain.getChain()) {
            for (Invoice inv : block.getInvoices()) {
                if (inv.getId().equals(invoice.getId()))
                    return true;
            }
        }
        return false;
    }

    public double getClaimedTotal(String businessId) {
        return claimedByBusiness.getOrDefault(businessId, 0.0);
    }

    /**
     * Demo: one legitimate claim succeeds, one claim on an unmined invoice is
     * rejected.
     */
    public static void main(String[] args) {
        Blockchain chain = new Blockchain(3);
        Wallet a = new Wallet(), b = new Wallet();

        Invoice minedInvoice = new Invoice("INV/A/001", "A", a.getAddress(), "B", b.getAddress(),
                java.util.List.of(new LineItem("Yarn", 100, 40, 18)));
        minedInvoice.signAsSeller(a.getPublicKeyBase64(), a.getPrivateKeyBase64());
        minedInvoice.signAsBuyer(b.getPublicKeyBase64(), b.getPrivateKeyBase64());
        chain.addBlock(java.util.List.of(minedInvoice), "Validator-Alpha");

        Invoice fakeInvoice = new Invoice("INV/A/999", "A", a.getAddress(), "B", b.getAddress(),
                java.util.List.of(new LineItem("Ghost Goods", 100, 40, 18)));
        fakeInvoice.signAsSeller(a.getPublicKeyBase64(), a.getPrivateKeyBase64());
        // never mined, never buyer-signed - this is the "fake invoice" scenario

        ITCLedger ledger = new ITCLedger();
        System.out.println("Claim on genuine mined invoice: " + ledger.claimITC("B", minedInvoice, chain));
        System.out.println("Claim on fake/unmined invoice : " + ledger.claimITC("B", fakeInvoice, chain));
        System.out.println("Total ITC claimed by B: Rs." + ledger.getClaimedTotal("B"));
    }
}