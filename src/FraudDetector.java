import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple fraud checks over a batch of invoices: circular trading
 * (goods "sold" in a loop back to the original seller, a classic fake
 * ITC trick) and businesses claiming more ITC than was ever invoiced.
 */
public class FraudDetector {

    /**
     * Detects a seller->buyer address cycle (A sells to B, B to C, C back to A).
     */
    public static boolean detectCircularTrading(List<Invoice> invoices) {
        Map<String, String> nextHop = new HashMap<>();
        for (Invoice inv : invoices)
            nextHop.put(inv.getSellerAddress(), inv.getBuyerAddress());

        for (String start : nextHop.keySet()) {
            Set<String> visited = new HashSet<>();
            String current = start;
            while (nextHop.containsKey(current)) {
                if (!visited.add(current))
                    break;
                current = nextHop.get(current);
                if (current.equals(start))
                    return true;
            }
        }
        return false;
    }

    /**
     * Flags a business whose claimed ITC exceeds the GST it was ever legitimately
     * invoiced.
     */
    public static boolean detectOverclaimedITC(double claimedTotal, double actuallyInvoicedTotal) {
        return claimedTotal > actuallyInvoicedTotal;
    }

    /**
     * Demo: three invoices forming a fraud loop A->B->C->A, plus an overclaim
     * example.
     */
    public static void main(String[] args) {
        Wallet a = new Wallet(), b = new Wallet(), c = new Wallet();

        Invoice i1 = new Invoice("INV/A/001", "A", a.getAddress(), "B", b.getAddress(),
                List.of(new LineItem("Steel", 10, 1000, 18)));
        Invoice i2 = new Invoice("INV/B/001", "B", b.getAddress(), "C", c.getAddress(),
                List.of(new LineItem("Steel", 10, 1000, 18)));
        Invoice i3 = new Invoice("INV/C/001", "C", c.getAddress(), "A", a.getAddress(),
                List.of(new LineItem("Steel", 10, 1000, 18)));

        boolean circular = detectCircularTrading(List.of(i1, i2, i3));
        System.out.println("Circular trading detected (A->B->C->A): " + circular);

        boolean overclaim = detectOverclaimedITC(50000, 30000);
        System.out.println("ITC overclaim detected (claimed Rs.50000 vs invoiced Rs.30000): " + overclaim);
    }
}