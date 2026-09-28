import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks every Input Tax Credit claim ever made, and only allows a new
 * claim if:
 *   1. the claimant is actually the BUYER on that invoice (only a buyer
 *      pays and can reclaim input tax - a seller has no ITC to claim on
 *      their own sale),
 *   2. the invoice is genuine (fully signed, signatures verify), and
 *      actually appears in a mined block, and
 *   3. that specific invoice has never already been claimed against -
 *      claiming the same invoice twice would double-count real tax credit.
 * This is the direct fix for fake and duplicated ITC claims in real GST fraud.
 */
public class ITCLedger {

    /** One record of an accepted claim - kept individually, not just summed, so a full claims ledger can be shown. */
    public static class ClaimRecord {
        public final String businessId;
        public final UUID invoiceId;
        public final String businessInvoiceNumber;
        public final double amount;
        public final long timestamp;

        ClaimRecord(String businessId, UUID invoiceId, String businessInvoiceNumber, double amount) {
            this.businessId = businessId;
            this.invoiceId = invoiceId;
            this.businessInvoiceNumber = businessInvoiceNumber;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final List<ClaimRecord> claims = new ArrayList<>();

    /** Every invoice id that has already had an ITC claim made against it - prevents double-claiming. */
    private final Set<UUID> alreadyClaimedInvoiceIds = new HashSet<>();

    /** Allows an ITC claim only if the claimant is the buyer, the invoice is genuine, mined, and unclaimed so far. */
    public synchronized boolean claimITC(String businessId, Invoice invoice, Blockchain chain) {
        if (!businessId.equals(invoice.getBuyerId())) return false;
        if (!invoice.verifySignatures()) return false;
        if (!invoiceExistsInChain(invoice, chain)) return false;
        if (!alreadyClaimedInvoiceIds.add(invoice.getId())) return false;

        claims.add(new ClaimRecord(businessId, invoice.getId(), invoice.getBusinessInvoiceNumber(), invoice.getTotalGstAmount()));
        return true;
    }

    private boolean invoiceExistsInChain(Invoice invoice, Blockchain chain) {
        for (Block block : chain.getChain()) {
            for (Invoice inv : block.getInvoices()) {
                if (inv.getId().equals(invoice.getId())) return true;
            }
        }
        return false;
    }

    public double getClaimedTotal(String businessId) {
        double total = 0;
        for (ClaimRecord c : claims) if (c.businessId.equals(businessId)) total += c.amount;
        return total;
    }

    public boolean isAlreadyClaimed(UUID invoiceId) {
        return alreadyClaimedInvoiceIds.contains(invoiceId);
    }

    public List<ClaimRecord> getAllClaims() {
        return claims;
    }

    /** Demo: a genuine claim, a rejected double-claim, a rejected wrong-party claim, and a rejected fake claim. */
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
        System.out.println("Buyer B claims genuine mined invoice     : " + ledger.claimITC("B", minedInvoice, chain));
        System.out.println("Buyer B claims the SAME invoice again    : " + ledger.claimITC("B", minedInvoice, chain) + "  (must be false - no double claiming)");
        System.out.println("Seller A tries to claim their own invoice: " + ledger.claimITC("A", minedInvoice, chain) + "  (must be false - only the buyer may claim)");
        System.out.println("Claim on fake/unmined invoice             : " + ledger.claimITC("B", fakeInvoice, chain) + "  (must be false)");
        System.out.println("Total ITC claimed by B: Rs." + ledger.getClaimedTotal("B") + " (should only count once)");
        System.out.println("Total claim records stored: " + ledger.getAllClaims().size() + " (should be exactly 1)");
    }
}
