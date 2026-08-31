/**
 * One line item within an Invoice - e.g. "50 units of Cotton Yarn at
 * Rs.200 each, taxed at 5%". An invoice can hold several of these,
 * each with its own quantity, price, and GST rate.
 */
public class LineItem {

    private final String itemName;
    private final double quantity;
    private final double unitPrice;
    private final double gstRate;

    public LineItem(
            String itemName,
            double quantity,
            double unitPrice,
            double gstRate) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.gstRate = gstRate;
    }

    public double getTaxableValue() {
        return quantity * unitPrice;
    }

    public double getGstAmount() {
        return getTaxableValue() * gstRate / 100.0;
    }

    public double getTotalWithGst() {
        return getTaxableValue() + getGstAmount();
    }

    public String getItemName() {
        return itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getGstRate() {
        return gstRate;
    }

    /**
     * A compact, deterministic string used when this item's data goes into an
     * invoice hash.
     */
    public String toHashString() {
        return itemName + "x" + quantity + "@" + unitPrice + "@" + gstRate + "%";
    }

    @Override
    public String toString() {
        return itemName + ": " + quantity + " x Rs." + unitPrice
                + " = Rs." + getTaxableValue()
                + " (+GST " + gstRate + "% = Rs." + getGstAmount() + ")";
    }

    /**
     * Demo: two line items with different GST rates, showing each is computed
     * independently.
     */
    public static void main(String[] args) {
        LineItem item1 = new LineItem("Cotton Yarn", 50, 200, 5);
        LineItem item2 = new LineItem("Packaging Boxes", 100, 30, 18);

        System.out.println(item1);
        System.out.println("Hash String: " + item1.toHashString());
        System.out.println();
        System.out.println(item2);
        System.out.println("Hash String: " + item2.toHashString());
        System.out.println();
        System.out.println("Combined taxable value: Rs."
                + (item1.getTaxableValue() + item2.getTaxableValue()));
        System.out.println("Combined GST amount   : Rs."
                + (item1.getGstAmount() + item2.getGstAmount()));
    }
}
