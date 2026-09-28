import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A GST-registered business: Manufacturer, Distributor, Retailer, etc.
 * Extends NetworkParticipant to get an id, name, and wallet for free.
 */
public class Business extends NetworkParticipant {

    /** Every GSTIN issued so far, shared across ALL Business objects, so no two ever collide. */
    private static final Set<String> issuedGstins = ConcurrentHashMap.newKeySet();

    /** Every business name already registered (case-insensitive), so no two businesses share a name. */
    private static final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

    private final String gstin;
    private final String businessType;

    public Business(String name, String businessType) {
        super(validateUniqueName(name));
        this.businessType = businessType;
        this.gstin = generateUniqueGstin();
    }

    /**
     * Rejects blank names and names already taken by another business.
     * Called as the argument to super(...), so it runs before the object
     * is even constructed - a duplicate name never gets this far.
     */
    private static String validateUniqueName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }
        String trimmed = name.trim();
        if (!registeredNames.add(trimmed.toLowerCase())) {
            throw new IllegalArgumentException("A business named '" + trimmed + "' is already registered");
        }
        return trimmed;
    }

    /** Keeps generating a random GSTIN until it finds one nobody else already has. */
    private static synchronized String generateUniqueGstin() {
        String candidate;
        do {
            candidate = randomGstin();
        } while (issuedGstins.contains(candidate));
        issuedGstins.add(candidate);
        return candidate;
    }

    /** Builds a fake-but-realistic-looking 15 character GSTIN for the simulation. */
    private static String randomGstin() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(10 + r.nextInt(28)); // fake state code
        for (int i = 0; i < 10; i++) sb.append((char) ('A' + r.nextInt(26)));
        sb.append("1Z").append(r.nextInt(10));
        return sb.toString();
    }

    public String getGstin() { return gstin; }
    public String getBusinessType() { return businessType; }

    @Override
    public String getRole() { return businessType; }

    /** Demo: create several businesses and confirm every GSTIN generated is unique. */
    public static void main(String[] args) {
        Business acme = new Business("Acme Textiles", "Manufacturer");
        System.out.println("Name: " + acme.getName());
        System.out.println("Role: " + acme.getRole());
        System.out.println("GSTIN: " + acme.getGstin());
        System.out.println("Address (from Wallet, via NetworkParticipant): " + acme.getAddress());
        System.out.println(acme);

        System.out.println();
        System.out.println("Creating 5,000 more businesses to check for GSTIN collisions...");
        for (int i = 0; i < 5000; i++) new Business("Business" + i, "Retailer");
        System.out.println("Total unique GSTINs issued: " + issuedGstins.size() + " (should be 5001)");

        System.out.println();
        System.out.println("Trying to register another business also named 'Acme Textiles'...");
        try {
            new Business("Acme Textiles", "Distributor");
            System.out.println("ERROR: duplicate name was NOT rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }
    }
}

