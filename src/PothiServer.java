import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main application: starts the HTTP server, serves the dashboard, and
 * exposes a REST API that connects the HTML frontend to every class
 * we built - Business, GSTNode, Invoice, Block, Blockchain, ITCLedger,
 * FraudDetector and Storage.
 */
public class PothiServer {

    static final int PORT = 8080;
    static final int DIFFICULTY = 4;

    static final Blockchain blockchain = new Blockchain(DIFFICULTY);
    static final ITCLedger itcLedger = new ITCLedger();
    static final Map<String, Business> businesses = new ConcurrentHashMap<>();
    static final Map<String, GSTNode> nodes = new ConcurrentHashMap<>();
    static final Map<String, Business> byAddress = new ConcurrentHashMap<>();
    static final List<Invoice> pending = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        nodes.put("seed1", new GSTNode("Validator-Alpha"));
        nodes.put("seed2", new GSTNode("Validator-Beta"));

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", PothiServer::serveFrontend);
        server.createContext("/api/state", cors(PothiServer::handleState));
        server.createContext("/api/business/create", cors(PothiServer::handleCreateBusiness));
        server.createContext("/api/node/create", cors(PothiServer::handleCreateNode));
        server.createContext("/api/invoice/create", cors(PothiServer::handleCreateInvoice));
        server.createContext("/api/invoice/acknowledge", cors(PothiServer::handleAcknowledge));
        server.createContext("/api/mine", cors(PothiServer::handleMine));
        server.createContext("/api/itc/claim", cors(PothiServer::handleClaimItc));
        server.createContext("/api/validate", cors(PothiServer::handleValidate));
        server.createContext("/api/fraud/check", cors(PothiServer::handleFraudCheck));

        server.setExecutor(null);
        server.start();
        System.out.println("GST POTHI W3 running at http://localhost:" + PORT);
    }

    // -------- frontend --------

    static void serveFrontend(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = Paths.get("web", path.substring(1));
        if (Files.exists(file)) {
            byte[] data = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        } else {
            send(ex, 404, "Not found");
        }
    }

    // -------- API handlers --------

    static void handleState(HttpExchange ex) throws IOException {
        List<String> bizJson = new ArrayList<>();
        for (Business b : businesses.values())
            bizJson.add(Json.obj("id", Json.str(b.getId()), "name", Json.str(b.getName()),
                    "role", Json.str(b.getRole()), "gstin", Json.str(b.getGstin()),
                    "address", Json.str(b.getAddress()), "publicKey", Json.str(b.getWallet().getPublicKeyBase64())));

        List<String> nodeJson = new ArrayList<>();
        for (Map.Entry<String, GSTNode> e : nodes.entrySet())
            nodeJson.add(Json.obj("id", Json.str(e.getKey()), "name", Json.str(e.getValue().getName()),
                    "blocksValidated", Json.num(e.getValue().getBlocksValidated())));

        List<String> blockJson = new ArrayList<>();
        List<Blockchain.BlockCheck> checks = blockchain.validateAll();
        for (Block blk : blockchain.getChain()) {
            Blockchain.BlockCheck check = checks.get(blk.getIndex());
            blockJson.add(blockToJson(blk, check));
        }

        List<String> pendingJson = new ArrayList<>();
        synchronized (pending) { for (Invoice inv : pending) pendingJson.add(invoiceToJson(inv)); }

        List<String> claimsJson = new ArrayList<>();
        for (ITCLedger.ClaimRecord c : itcLedger.getAllClaims()) {
            Business claimant = businesses.get(c.businessId);
            claimsJson.add(Json.obj(
                    "businessId", Json.str(c.businessId),
                    "businessName", Json.str(claimant == null ? "unknown" : claimant.getName()),
                    "invoiceId", Json.str(c.invoiceId.toString()),
                    "businessInvoiceNumber", Json.str(c.businessInvoiceNumber),
                    "amount", Json.num(c.amount),
                    "timestamp", Json.num(c.timestamp)));
        }

        Blockchain.ValidationResult vr = blockchain.validate();
        String json = Json.obj(
                "difficulty", Json.num(DIFFICULTY),
                "businesses", Json.arr(bizJson),
                "nodes", Json.arr(nodeJson),
                "chain", Json.arr(blockJson),
                "pending", Json.arr(pendingJson),
                "itcClaims", Json.arr(claimsJson),
                "chainValid", Json.bool(vr.valid),
                "chainMessage", Json.str(vr.message)
        );
        try { Storage.save(json); } catch (IOException ignored) {}
        send(ex, 200, json);
    }

    static void handleCreateBusiness(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        Business b = new Business(f.getOrDefault("name", "Unnamed"), f.getOrDefault("type", "Manufacturer"));
        businesses.put(b.getId(), b);
        byAddress.put(b.getAddress(), b);
        send(ex, 200, Json.obj("id", Json.str(b.getId()), "address", Json.str(b.getAddress()),
                "privateKey", Json.str(b.getWallet().getPrivateKeyBase64())));
    }

    static void handleCreateNode(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        GSTNode n = new GSTNode(f.getOrDefault("name", "Validator"));
        String id = UUID.randomUUID().toString().substring(0, 6);
        nodes.put(id, n);
        send(ex, 200, Json.obj("id", Json.str(id), "name", Json.str(n.getName())));
    }

    /**
     * Items arrive as one form field "items", formatted as:
     *   name|qty|unitPrice|gstRate;name2|qty2|unitPrice2|gstRate2;...
     * A plain, dependency-free way to send a variable number of line items
     * through a single HTML form field. The frontend builds this string.
     */
    static List<LineItem> parseItems(String raw) {
        List<LineItem> items = new ArrayList<>();
        for (String part : raw.split(";")) {
            if (part.isBlank()) continue;
            String[] f = part.split("\\|");
            items.add(new LineItem(f[0], Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3])));
        }
        return items;
    }

    static void handleCreateInvoice(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        Business seller = businesses.get(f.get("sellerId"));
        Business buyer = businesses.get(f.get("buyerId"));
        if (seller == null || buyer == null) { send(ex, 400, Json.obj("error", Json.str("Unknown business"))); return; }

        List<LineItem> items = parseItems(f.get("items"));
        Invoice inv = new Invoice(f.get("businessInvoiceNumber"), seller.getId(), seller.getAddress(),
                buyer.getId(), buyer.getAddress(), items);
        inv.signAsSeller(seller.getWallet().getPublicKeyBase64(), f.get("sellerPrivateKey"));
        pending.add(inv);
        send(ex, 200, invoiceToJson(inv));
    }

    static void handleAcknowledge(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        Invoice inv = findPending(f.get("invoiceId"));
        Business buyer = businesses.get(f.get("buyerId"));
        if (inv == null || buyer == null) { send(ex, 400, Json.obj("error", Json.str("Not found"))); return; }
        inv.signAsBuyer(buyer.getWallet().getPublicKeyBase64(), f.get("buyerPrivateKey"));
        send(ex, 200, invoiceToJson(inv));
    }

    static void handleMine(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        GSTNode node = nodes.get(f.get("nodeId"));
        if (node == null) { send(ex, 400, Json.obj("error", Json.str("Unknown node"))); return; }

        List<Invoice> ready = new ArrayList<>();
        synchronized (pending) {
            // isFullySigned() should already imply verifySignatures()==true, since signing verifies
            // immediately - but this second check costs nothing and guarantees a corrupt invoice
            // can never be mined into a block, even if some future code path skipped that check.
            for (Invoice inv : pending) if (inv.isFullySigned() && inv.verifySignatures()) ready.add(inv);
            pending.removeAll(ready);
        }
        if (ready.isEmpty()) { send(ex, 400, Json.obj("error", Json.str("No fully-signed invoices to mine"))); return; }

        Block block = blockchain.addBlock(ready, node.getName());
        Blockchain.BlockCheck check = blockchain.validateAll().get(block.getIndex());
        send(ex, 200, blockToJson(block, check));
    }

    static void handleClaimItc(HttpExchange ex) throws IOException {
        Map<String, String> f = Json.parseForm(readBody(ex));
        Invoice inv = findAnywhere(f.get("invoiceId"));
        if (inv == null) { send(ex, 400, Json.obj("error", Json.str("Invoice not found"))); return; }
        boolean ok = itcLedger.claimITC(f.get("businessId"), inv, blockchain);
        send(ex, 200, Json.obj("accepted", Json.bool(ok),
                "totalClaimed", Json.num(itcLedger.getClaimedTotal(f.get("businessId")))));
    }

    static void handleValidate(HttpExchange ex) throws IOException {
        List<String> reports = new ArrayList<>();
        for (GSTNode n : nodes.values()) {
            Blockchain.ValidationResult r = n.validate(blockchain);
            reports.add(Json.obj("node", Json.str(n.getName()), "valid", Json.bool(r.valid), "message", Json.str(r.message)));
        }
        send(ex, 200, Json.obj("reports", Json.arr(reports)));
    }

    /** Scans every MINED (confirmed) invoice for circular trading and ITC overclaims. */
    static void handleFraudCheck(HttpExchange ex) throws IOException {
        List<Invoice> confirmed = new ArrayList<>();
        for (Block b : blockchain.getChain()) confirmed.addAll(b.getInvoices());

        boolean circular = FraudDetector.detectCircularTrading(confirmed);

        // ITC is claimed by the BUYER on what they were actually invoiced for.
        Map<String, Double> invoicedAsBuyer = new HashMap<>();
        for (Invoice inv : confirmed) invoicedAsBuyer.merge(inv.getBuyerId(), inv.getTotalGstAmount(), Double::sum);

        List<String> overclaims = new ArrayList<>();
        for (Business b : businesses.values()) {
            double claimed = itcLedger.getClaimedTotal(b.getId());
            double actuallyInvoiced = invoicedAsBuyer.getOrDefault(b.getId(), 0.0);
            if (FraudDetector.detectOverclaimedITC(claimed, actuallyInvoiced)) {
                overclaims.add(Json.obj("businessId", Json.str(b.getId()), "name", Json.str(b.getName()),
                        "claimed", Json.num(claimed), "actuallyInvoiced", Json.num(actuallyInvoiced)));
            }
        }
        send(ex, 200, Json.obj("circularTradingDetected", Json.bool(circular), "overclaims", Json.arr(overclaims)));
    }

    // -------- helpers --------

    /** Looked up by internal id, not businessInvoiceNumber - two sellers could reuse the same number. */
    static Invoice findPending(String id) {
        synchronized (pending) { for (Invoice i : pending) if (i.getId().toString().equals(id)) return i; }
        return null;
    }

    static Invoice findAnywhere(String id) {
        Invoice p = findPending(id);
        if (p != null) return p;
        for (Block b : blockchain.getChain())
            for (Invoice i : b.getInvoices()) if (i.getId().toString().equals(id)) return i;
        return null;
    }

    static String invoiceToJson(Invoice inv) {
        List<String> itemsJson = new ArrayList<>();
        for (LineItem item : inv.getItems()) {
            itemsJson.add(Json.obj("itemName", Json.str(item.getItemName()), "quantity", Json.num(item.getQuantity()),
                    "unitPrice", Json.num(item.getUnitPrice()), "gstRate", Json.num(item.getGstRate()),
                    "taxableValue", Json.num(item.getTaxableValue()), "gstAmount", Json.num(item.getGstAmount())));
        }
        Business seller = businesses.get(inv.getSellerId());
        Business buyer = businesses.get(inv.getBuyerId());
        return Json.obj("id", Json.str(inv.getId().toString()),
                "businessInvoiceNumber", Json.str(inv.getBusinessInvoiceNumber()),
                "items", Json.arr(itemsJson),
                "sellerName", Json.str(seller == null ? "unknown" : seller.getName()),
                "buyerName", Json.str(buyer == null ? "unknown" : buyer.getName()),
                "sellerAddress", Json.str(inv.getSellerAddress()), "buyerAddress", Json.str(inv.getBuyerAddress()),
                "totalTaxableValue", Json.num(inv.getTotalTaxableValue()),
                "totalGstAmount", Json.num(inv.getTotalGstAmount()),
                "grandTotal", Json.num(inv.getGrandTotal()),
                "fullySigned", Json.bool(inv.isFullySigned()),
                "signaturesValid", Json.bool(inv.isFullySigned() && inv.verifySignatures()),
                "alreadyClaimed", Json.bool(itcLedger.isAlreadyClaimed(inv.getId())));
    }

    static String blockToJson(Block b, Blockchain.BlockCheck check) {
        List<String> invs = new ArrayList<>();
        for (Invoice i : b.getInvoices()) invs.add(invoiceToJson(i));
        return Json.obj("index", Json.num(b.getIndex()), "hash", Json.str(b.getHash()),
                "previousHash", Json.str(b.getPreviousHash()), "nonce", Json.num(b.getNonce()),
                "minedBy", Json.str(b.getMinedBy()), "invoices", Json.arr(invs),
                "isValid", Json.bool(check.isValid()), "reason", Json.str(check.reason()));
    }

    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    static HttpHandler cors(ThrowingHandler inner) {
        return ex -> {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            try { inner.handle(ex); }
            catch (IllegalArgumentException e) { send(ex, 400, Json.obj("error", Json.str(e.getMessage()))); }
            catch (Exception e) { send(ex, 500, Json.obj("error", Json.str("" + e.getMessage()))); }
        };
    }

    interface ThrowingHandler { void handle(HttpExchange ex) throws Exception; }
}
