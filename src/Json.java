import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer plus a form-data parser, written by hand so the
 * whole project needs nothing beyond the plain JDK.
 */
public final class Json {

    private Json() {
    }

    public static String esc(String s) {
        if (s == null)
            return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"')
                b.append("\\\"");
            else if (c == '\\')
                b.append("\\\\");
            else if (c == '\n')
                b.append("\\n");
            else
                b.append(c);
        }
        return b.toString();
    }

    public static String str(String v) {
        return "\"" + esc(v) + "\"";
    }

    public static String num(double n) {
        return String.valueOf(n);
    }

    public static String bool(boolean b) {
        return String.valueOf(b);
    }

    /**
     * Builds a JSON object from alternating key, value pairs:
     * obj("name", str("bob")).
     */
    public static String obj(Object... kv) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0)
                b.append(",");
            b.append(str(String.valueOf(kv[i]))).append(":").append(kv[i + 1]);
        }
        return b.append("}").toString();
    }

    public static String arr(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    /** Parses application/x-www-form-urlencoded bodies into a key-value map. */
    public static Map<String, String> parseForm(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null || body.isEmpty())
            return map;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            try {
                String key = java.net.URLDecoder.decode(parts[0], "UTF-8");
                String val = parts.length > 1 ? java.net.URLDecoder.decode(parts[1], "UTF-8") : "";
                map.put(key, val);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    /** Demo: build a small JSON object and parse a sample form string. */
    public static void main(String[] args) {
        String json = obj("name", str("Acme"), "amount", num(5000), "valid", bool(true));
        System.out.println("Built JSON: " + json);

        Map<String, String> form = parseForm("name=Acme+Textiles&amount=5000");
        System.out.println("Parsed form: " + form);
    }
}
