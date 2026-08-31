import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and loads a JSON snapshot of the whole app state to a local file.
 * This is a placeholder persistence layer - it will be swapped for an
 * SQLite-backed version later without changing any other class.
 */
public class Storage {

    /** Real file used by the running app (PothiServer). */
    private static final Path FILE_PATH = Path.of("pothi-data.json");

    /**
     * Separate file used ONLY by this class's own demo below, so running
     * "java Storage" by itself can never overwrite the real app's saved data.
     */
    private static final Path DEMO_FILE_PATH = Path.of("pothi-data-DEMO.json");

    public static void save(String jsonContent) throws IOException {
        Files.writeString(FILE_PATH, jsonContent);
    }

    /** Returns the saved JSON, or null if no snapshot exists yet. */
    public static String load() throws IOException {
        if (!Files.exists(FILE_PATH))
            return null;
        return Files.readString(FILE_PATH);
    }

    /**
     * Demo: save a small snapshot to a separate demo file, read it back, then
     * delete it.
     */
    public static void main(String[] args) throws IOException {
        String snapshot = Json.obj("businesses", Json.num(2), "blocks", Json.num(1));

        Files.writeString(DEMO_FILE_PATH, snapshot);
        System.out.println("Saved demo snapshot to " + DEMO_FILE_PATH.toAbsolutePath());

        String loaded = Files.readString(DEMO_FILE_PATH);
        System.out.println("Loaded back: " + loaded);

        Files.deleteIfExists(DEMO_FILE_PATH);
        System.out.println("Demo file cleaned up.");
    }
}