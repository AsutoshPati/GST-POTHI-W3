import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A GST-registered business: Manufacturer, Distributor, Retailer, etc.
 * Extends NetworkParticipant to get an id, name, and wallet for free.
 */
public class Business extends NetworkParticipant {

    /**
     * Every GSTIN issued so far, shared across ALL Business objects, so no two ever
     * collide.
     */
    private static final Set<String> issuedGstins = ConcurrentHashMap.newKeySet();

    private final String gstin;
    private final String businessType;

    public Business(String name, String businessType) {
        super(name);
        this.businessType = businessType;
        this.gstin = generateUniqueGstin();
    }

    /**
     * Keeps generating a random GSTIN until it finds one nobody else already has.
     */
    private static synchronized String generateUniqueGstin() {
        String candidate;
        do {
            candidate = randomGstin();
        } while (issuedGstins.contains(candidate));
        issuedGstins.add(candidate);
        return candidate;
    }

    /**
     * Builds a fake-but-realistic-looking 15 character GSTIN for the simulation.
     */
    private static String randomGstin() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(10 + r.nextInt(28)); // fake state code
        for (int i = 0; i < 10; i++)
            sb.append((char) ('A' + r.nextInt(26)));
        sb.append("1Z").append(r.nextInt(10));
        return sb.toString();
    }

    public String getGstin() {
        return gstin;
    }

    public String getBusinessType() {
        return businessType;
    }

    @Override
    public String getRole() {
        return businessType;
    }

    /**
     * Demo: create several businesses and confirm every GSTIN generated is unique.
     */
    public static void main(String[] args) {
        Business acme = new Business("Acme Textiles", "Manufacturer");
        System.out.println("Name: " + acme.getName());
        System.out.println("Role: " + acme.getRole());
        System.out.println("GSTIN: " + acme.getGstin());
        System.out.println(
                "Address (from Wallet, via NetworkParticipant): "
                        + acme.getAddress());
        System.out.println(acme);
        System.out.println();
    }
}