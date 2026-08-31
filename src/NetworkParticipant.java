import java.util.UUID;

/**
 * Abstract base for anyone with an identity on the network.
 * 
 * Subclasses define the participant's role while this class provides
 * shared identity, name, and wallet information.
 */
public abstract class NetworkParticipant {

    protected final String id;
    protected final String name;
    protected final Wallet wallet;

    protected NetworkParticipant(String name) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.wallet = new Wallet();
    }

    /**
     * Subclass says what kind of participant this is (e.g. "Manufacturer",
     * "Validator").
     */
    public abstract String getRole();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public String getAddress() {
        return wallet.getAddress();
    }

    @Override
    public String toString() {
        return name + " [" + getRole() + "] address=" + getAddress();
    }

    /**
     * Demo: NetworkParticipant is abstract, so we make one throwaway subclass to
     * show it in action.
     */
    public static void main(String[] args) {
        NetworkParticipant demoUser = new NetworkParticipant("Demo Participant") {
            public String getRole() {
                return "Generic";
            }
        };
        System.out.println(demoUser);
        System.out.println("Public key: " + demoUser.getWallet().getPublicKeyBase64().substring(0, 30) + "...");
    }
}
