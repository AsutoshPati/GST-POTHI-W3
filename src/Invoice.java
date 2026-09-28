import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One GST invoice between a seller and a buyer, made up of one or more
 * LineItems (an invoice usually has several products/services, each
 * with its own quantity, price, and GST rate).
 *
 * Extends Hashable to get tamper-evident hashing for free, plus a
 * unique internal id (see Hashable.getId()).
 *
 * Every invoice carries TWO identifiers:
 *  - id (inherited from Hashable)   -> our internal system reference,
 *                                       guaranteed unique, never shown to
 *                                       the business
 *  - businessInvoiceNumber           -> the seller's OWN invoice number,
 *                                       in whatever format their business
 *                                       already uses (e.g. "INV/2026/0042").
 *                                       Must be unique PER SELLER, just like
 *                                       a real business never reuses its own
 *                                       invoice numbers.
 *
 * A real transaction needs BOTH the seller and the buyer to sign it -
 * that is what makes one-sided, fake shell-company invoices hard to create.
 * Both signAsSeller() and signAsBuyer() verify the signature immediately,
 * so a wrong or mismatched private key is rejected on the spot rather than
 * silently accepted and only caught much later during block validation.
 */
public class Invoice extends Hashable {

    /** "sellerId::businessInvoiceNumber" pairs already used, so no seller can reuse a number. */
    private static final Set<String> issuedInvoiceKeys = ConcurrentHashMap.newKeySet();

    private final String businessInvoiceNumber;
    private final String sellerId, sellerAddress;
    private final String buyerId, buyerAddress;
    private final List<LineItem> items;

    private String sellerPublicKey, sellerSignature;
    private String buyerPublicKey, buyerSignature;

    public Invoice(String businessInvoiceNumber, String sellerId, String sellerAddress,
                   String buyerId, String buyerAddress, List<LineItem> items) {
        super();
        if (businessInvoiceNumber == null || businessInvoiceNumber.isBlank()) {
            throw new IllegalArgumentException("businessInvoiceNumber is required - every seller has their own numbering");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An invoice needs at least one line item");
        }
        if (!issuedInvoiceKeys.add(sellerId + "::" + businessInvoiceNumber)) {
            throw new IllegalArgumentException(
                    "Seller has already used invoice number '" + businessInvoiceNumber + "'");
        }
        this.businessInvoiceNumber = businessInvoiceNumber;
        this.sellerId = sellerId;
        this.sellerAddress = sellerAddress;
        this.buyerId = buyerId;
        this.buyerAddress = buyerAddress;
        this.items = items;
    }

    /** The fields that go into this invoice's hash - order matters, every line item is included. */
    @Override
    protected String getDataToHash() {
        StringBuilder itemData = new StringBuilder();
        for (LineItem item : items) itemData.append(item.toHashString()).append(";");
        return id + "|" + businessInvoiceNumber + "|" + sellerAddress + "|" + buyerAddress + "|"
                + itemData + "|" + timestamp;
    }

    /**
     * Seller signs the invoice with their private key.
     * Verifies the signature against the given public key BEFORE accepting it -
     * a wrong or unrelated private key fails right here, not later at mining time.
     */
    public void signAsSeller(String publicKey, String privateKey) {
        String signature = Wallet.sign(privateKey, getDataToHash());
        if (!Wallet.verify(publicKey, getDataToHash(), signature)) {
            throw new IllegalArgumentException("Seller signature does not match the seller's public key - wrong private key?");
        }
        this.sellerPublicKey = publicKey;
        this.sellerSignature = signature;
    }

    /** Buyer co-signs to acknowledge the invoice really happened - same immediate check as the seller. */
    public void signAsBuyer(String publicKey, String privateKey) {
        String signature = Wallet.sign(privateKey, getDataToHash());
        if (!Wallet.verify(publicKey, getDataToHash(), signature)) {
            throw new IllegalArgumentException("Buyer signature does not match the buyer's public key - wrong private key?");
        }
        this.buyerPublicKey = publicKey;
        this.buyerSignature = signature;
    }

    public boolean isFullySigned() {
        return sellerSignature != null && buyerSignature != null;
    }

    /** Checks both signatures are genuine and match this invoice's data. */
    public boolean verifySignatures() {
        if (!isFullySigned()) return false;
        boolean sellerOk = Wallet.verify(sellerPublicKey, getDataToHash(), sellerSignature);
        boolean buyerOk = Wallet.verify(buyerPublicKey, getDataToHash(), buyerSignature);
        return sellerOk && buyerOk;
    }

    /** Sum of every line item's taxable value (before GST). */
    public double getTotalTaxableValue() {
        double total = 0;
        for (LineItem item : items) total += item.getTaxableValue();
        return total;
    }

    /** Sum of every line item's GST amount - different items can have different GST rates. */
    public double getTotalGstAmount() {
        double total = 0;
        for (LineItem item : items) total += item.getGstAmount();
        return total;
    }

    public double getGrandTotal() {
        return getTotalTaxableValue() + getTotalGstAmount();
    }

    public List<LineItem> getItems() { return items; }
    public String getBusinessInvoiceNumber() { return businessInvoiceNumber; }
    public String getSellerId() { return sellerId; }
    public String getSellerAddress() { return sellerAddress; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerAddress() { return buyerAddress; }

    @Override
    public String toString() {
        return businessInvoiceNumber + " (internal id " + id + "): " + items.size() + " item(s), "
                + "taxable Rs." + getTotalTaxableValue() + ", GST Rs." + getTotalGstAmount()
                + ", signed=" + isFullySigned();
    }

    /** Demo: an invoice with two line items at different GST rates, signed by both parties. */
    public static void main(String[] args) {
        Wallet sellerWallet = new Wallet();
        Wallet buyerWallet = new Wallet();

        List<LineItem> items = List.of(
                new LineItem("Cotton Yarn", 50, 200, 5),
                new LineItem("Packaging Boxes", 100, 30, 18)
        );

        Invoice inv = new Invoice("INV/2026/0042", "SELLER1", sellerWallet.getAddress(),
                "BUYER1", buyerWallet.getAddress(), items);

        System.out.println("Business invoice number: " + inv.getBusinessInvoiceNumber());
        System.out.println("Internal system id      : " + inv.getId());
        System.out.println("Line items:");
        for (LineItem item : inv.getItems()) System.out.println("  " + item);
        System.out.println();
        System.out.println("Total taxable value: Rs." + inv.getTotalTaxableValue());
        System.out.println("Total GST amount   : Rs." + inv.getTotalGstAmount());
        System.out.println("Grand total        : Rs." + inv.getGrandTotal());
        System.out.println();
        inv.signAsSeller(sellerWallet.getPublicKeyBase64(), sellerWallet.getPrivateKeyBase64());
        inv.signAsBuyer(buyerWallet.getPublicKeyBase64(), buyerWallet.getPrivateKeyBase64());
        System.out.println(inv);
        System.out.println("Signatures valid: " + inv.verifySignatures());

        System.out.println();
        System.out.println("Trying to sign a NEW invoice with the wrong private key...");
        Invoice inv2 = new Invoice("INV/2026/0043", "SELLER1", sellerWallet.getAddress(),
                "BUYER1", buyerWallet.getAddress(), items);
        try {
            // signing "as seller" but actually using the buyer's private key by mistake
            inv2.signAsSeller(sellerWallet.getPublicKeyBase64(), buyerWallet.getPrivateKeyBase64());
            System.out.println("ERROR: mismatched key was NOT rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected at signing time: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Trying to reuse invoice number INV/2026/0042 for the same seller...");
        try {
            new Invoice("INV/2026/0042", "SELLER1", sellerWallet.getAddress(), "BUYER1", buyerWallet.getAddress(), items);
            System.out.println("ERROR: duplicate invoice number was NOT rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }
    }
}
