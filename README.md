# GST POTHI W3

**Genuine · Signed · Trustworthy**
*Proof-Oriented Transaction History & Integrity — a Web3 simulation for GST invoice verification and ITC fraud detection.*

> Traditional GST says "trust the authority." GST POTHI says "verify the evidence."

---

## Table of Contents

1. [Topic](#topic)
2. [Abstract / Brief](#abstract--brief)
3. [Existing Ecosystem](#existing-ecosystem)
4. [Loopholes in the Existing System](#loopholes-in-the-existing-system)
5. [How This Project Solves It](#how-this-project-solves-it)
6. [Tech Stack & Requirements](#tech-stack--requirements)
7. [Project Structure](#project-structure)
8. [Installation](#installation)
9. [Classes and Their Functionality](#classes-and-their-functionality)
10. [How to Use](#how-to-use)
11. [API Reference](#api-reference)
12. [Sample Walkthrough](#sample-walkthrough)
13. [Limitations & Disclaimer](#limitations--disclaimer)
14. [Future Integration and Possibilities](#future-integration-and-possibilities)
15. [Credits](#credits)

---

## Topic

**A Web3 (blockchain-based) simulation for verifying GST invoices between businesses, and for detecting fake Input Tax Credit (ITC) claims — built to demonstrate how cryptographic trust could reduce a real, large-scale tax fraud problem in India's GST system.**

---

## Abstract / Brief

GST POTHI W3 is a Java + HTML simulation of a blockchain-backed invoice verification network. Every business on the network gets a cryptographic wallet (a real EC keypair). When a seller issues an invoice, **both the seller and the buyer must digitally sign it** before it is considered genuine — a fake, one-sided invoice from a shell company cannot get a real counterparty's signature. Signed invoices are bundled into blocks, mined using proof-of-work, and chained together with tamper-evident SHA-256 hashes. Multiple simulated GST validator nodes independently re-check the entire chain — hashes, proof-of-work, and every signature — so trust doesn't depend on any single central authority. An ITC ledger only allows a business to claim Input Tax Credit if a genuine, mined, fully-signed invoice backs the claim, and a fraud detector scans the confirmed chain for classic fraud patterns like circular trading.

This is a **coursework simulation** — no real cryptocurrency, no real GSTINs, and no real tax filings are involved anywhere in this project. The cryptography (SHA-256 hashing, ECDSA digital signatures) is genuinely real; only the "currency" and "tax authority" concepts are simulated.

---

## Existing Ecosystem

India's GST system runs on a **self-reporting + credit-matching model**:

- A seller issues an invoice, charges GST, and reports it in **GSTR-1** (outward supplies).
- The buyer sees that invoice reflected in their **GSTR-2A/2B** (auto-populated from the seller's filing) and claims **Input Tax Credit (ITC)** — they only pay tax on the value they added, since tax on the input was "already paid" by the seller.
- Movement of goods above a threshold value requires an **E-way bill**.
- Since 2021, larger businesses must generate **e-invoices** through a government portal (an Invoice Reference Number, or IRN) before the invoice is legally valid.
- Reconciliation between GSTR-1, GSTR-2A/2B, and GSTR-3B is mostly a **manual cross-checking exercise** left to businesses and their accountants.

The entire system depends on one assumption: that every invoice corresponds to a *real* transaction where goods or services actually moved.

---

## Loopholes in the Existing System

That assumption is exactly where the fraud happens, and the numbers are large:

- Central tax authorities detected fraudulent ITC claims worth **₹74,782 crore during FY 2025–26 alone**.
- The Directorate General of GST Intelligence (DGGI) detected fake ITC worth **over ₹1.14 lakh crore between 2020 and 2025**, involving more than **15,000 fake entities**.
- Total GST evasion detected in FY 2024–25 crossed **₹2.01 lakh crore**.

**Common fraud patterns:**
- **Shell companies** — registered on forged/stolen identity documents, existing only on paper, issuing invoices for goods that never moved.
- **Circular trading** — an invoice is passed through a chain of companies (A → B → C → back to A) across states, so the final entity claims ITC on a transaction where nothing real was ever exchanged.
- **Inflated billing** — invoicing far more units than were actually sold, to claim inflated credit.
- **Export fraud** — fabricating invoices to build artificial ITC that is later claimed as a cash refund.

**Where current defenses fall short:**
- GSTN's AI risk engine mostly flags anomalies *after* fake credit has already been claimed — detection, not prevention.
- Honest, small buyers can face ITC reversal even when they acted in good faith, simply because a supplier several links upstream turns out to be a fraudulent shell company.
- Reconciliation is still largely a manual burden pushed onto businesses and their accountants.

---

## How This Project Solves It

GST POTHI W3's architecture maps directly onto these problems:

| Real-world problem | How GST POTHI addresses it |
|---|---|
| Forged / fake invoices from shell companies | Every invoice needs **both** a seller's and a genuine buyer's private-key signature — a one-sided fake invoice cannot pass `verifySignatures()` |
| Circular trading (A→B→C→A) | `FraudDetector.detectCircularTrading()` scans the confirmed chain and flags address loops |
| Fake / inflated ITC claims | `ITCLedger.claimITC()` only accepts a claim if the invoice is genuine, fully signed, **and actually mined into the chain** |
| ITC claimed exceeding what was ever invoiced | `FraudDetector.detectOverclaimedITC()` compares claimed totals against real invoiced totals |
| Siloed, single-authority record keeping | Multiple `GSTNode` validators independently re-verify the **entire** chain — hash linkage, proof-of-work, and every signature |
| Manual, after-the-fact reconciliation | Verification happens **at transaction time** — a bad invoice is rejected before it can ever be mined or claimed against |
| Backdating / editing old records | Every `Block` and `Invoice` extends `Hashable` — changing even one field completely changes the SHA-256 hash, and every node catches the mismatch instantly |

The core philosophical shift: instead of **detecting fraud after the fact** by mining a mountain of self-reported paperwork, GST POTHI makes fraud **structurally hard to create in the first place** — because every invoice needs a real, cryptographically provable counterparty.

**Benefit for small businesses specifically:** no manual GSTR-1 vs 2A/2B reconciliation (a signed transaction on both sides *is* the reconciliation), earlier visibility into an upstream supplier's questionable chain (before ITC reversal notices arrive), and an instant, provable audit trail instead of a filing cabinet of scanned invoices.

---

## Tech Stack & Requirements

- **Backend:** Java (JDK 17+ recommended), using only the standard library — no Maven, no Gradle, no external `.jar` files. HTTP is served via the built-in `com.sun.net.httpserver.HttpServer`.
- **Frontend:** Plain HTML, CSS, and JavaScript (`web/index.html`) — no build step, no framework, no npm install.
- **Cryptography:** `java.security` — real SHA-256 hashing and real ECDSA (secp256r1) digital signatures.
- **Persistence:** A local JSON snapshot file (see [`Storage.java`](#classes-and-their-functionality)) — planned to be swapped for SQLite (see [Future Integration](#future-integration-and-possibilities)).

You only need a JDK installed. Nothing else.

---

## Project Structure

```
GST-POTHI-W3/
├── src/
│   ├── CommonUtils.java        shared static helpers (hashing, etc.)
│   ├── Hashable.java           abstract base: tamper-evident fingerprinting
│   ├── NetworkParticipant.java abstract base: identity (id, name, wallet)
│   ├── Wallet.java             ECDSA keypair generation, signing, verification
│   ├── Business.java           extends NetworkParticipant - a GST-registered business
│   ├── GSTNode.java            extends NetworkParticipant - a validator node
│   ├── LineItem.java           one product/service line within an invoice
│   ├── Invoice.java            extends Hashable - a dual-signed, multi-item invoice
│   ├── Block.java              extends Hashable - proof-of-work mined block
│   ├── Blockchain.java         the chain + full validation logic
│   ├── ITCLedger.java          tracks and validates Input Tax Credit claims
│   ├── FraudDetector.java      circular trading & ITC overclaim detection
│   ├── Json.java               hand-rolled JSON writer + form parser
│   ├── Storage.java            JSON file persistence
│   └── PothiServer.java        main HTTP server, ties everything together
├── web/
│   └── index.html              the dashboard (HTML + CSS + JS, no build step)
└── README.md
```

---

## Installation

You need a JDK (17 or newer) installed. No other setup is required.

```bash
# 1. Move into the project folder
cd GST-POTHI-W3

# 2. Compile every class
javac -d out src/*.java

# 3. Run the server (serves both the API and the web/ dashboard)
java -cp out PothiServer
```

Then open **http://localhost:8080** in your browser.

> Run the `java` command from inside the `GST-POTHI-W3/` folder — the server looks for the `web/` folder relative to your current directory.

**Running a single class on its own** (every class has a standalone demo in its `main()` method):

```bash
java -cp out Wallet          # or Invoice, Block, Blockchain, FraudDetector, etc.
```

---

## Classes and Their Functionality

### Two small inheritance hierarchies

**`Hashable`** *(abstract)* — anything needing a tamper-evident fingerprint.
- Fields: `id` (UUID), `timestamp`
- `getDataToHash()` *(abstract)* — subclass says what to hash
- `computeHash()` — SHA-256 of that data (delegates to `CommonUtils.sha256()`)
- → **`Invoice extends Hashable`**
- → **`Block extends Hashable`**

**`NetworkParticipant`** *(abstract)* — anyone with an identity on the network.
- Fields: `id`, `name`, `wallet`
- `getRole()` *(abstract)*
- → **`Business extends NetworkParticipant`**
- → **`GSTNode extends NetworkParticipant`**

### Every class, one line each

| Class | What it does |
|---|---|
| `CommonUtils` | General-purpose static helpers shared across the project (currently `sha256()`) |
| `Hashable` | Abstract base providing tamper-evident SHA-256 fingerprints |
| `NetworkParticipant` | Abstract base providing identity (id, name, wallet) |
| `Wallet` | Generates a real ECDSA keypair; signs and verifies data; derives a short address from the public key |
| `Business` | A GST-registered participant (Manufacturer/Distributor/Retailer/Auditor) with a guaranteed-unique simulated GSTIN |
| `GSTNode` | A validator node (simulating a regional GST office) that independently re-checks the whole chain |
| `LineItem` | One product/service line within an invoice — its own quantity, unit price, and GST rate |
| `Invoice` | A dual-signed invoice made of one or more `LineItem`s, carrying both an internal system `id` and the business's own `businessInvoiceNumber` |
| `Block` | Bundles invoices, links to the previous block by hash, mined via proof-of-work |
| `Blockchain` | The ordered chain of blocks; `validate()` reports the first broken block, and `validateAll()` gives a per-block report (used by the Chain Tree) |
| `ITCLedger` | Keeps every ITC claim as a record; accepts a claim only from the invoice's buyer, for a genuine mined invoice, once per invoice |
| `FraudDetector` | Scans confirmed invoices for circular trading loops and ITC overclaims |
| `Json` | Hand-rolled JSON writer and form-data parser — no external dependency |
| `Storage` | Saves/loads a JSON snapshot of app state to disk (placeholder for a future SQLite version) |
| `PothiServer` | The main application — starts the HTTP server and wires every class above into a REST API |

---

## How to Use

The dashboard has five tabs. Each tab shows **only** what belongs to it, and returns to its default state (empty forms, no leftover messages, no open pop-ups) whenever you leave it.

1. **Identities** — Register businesses (real keypair + unique simulated GSTIN; copy the private key shown) and validator nodes. The right side lists only registered businesses and nodes. Names must be unique (case-insensitive).
2. **Invoices** — Issue a multi-line-item invoice signed by the seller, then have the buyer co-sign it. The right side lists every invoice; **click any row to open it as a full invoice document** (parties, itemised GST table, totals, signature stamps). A wrong private key is rejected immediately, and a seller cannot reuse an invoice number.
3. **Mine & Validate** — Mine fully-signed invoices into a block and run the multi-node consensus check. The right side shows the mempool and the confirmed blocks.
4. **ITC & Fraud** — Claim Input Tax Credit (only the invoice's buyer, only once per invoice, only if mined and genuine; already-claimed invoices are greyed out) and run the fraud scan. The right side shows the full ITC claims ledger.
5. **Chain Tree** — The blockchain drawn as a tree: each block is a trunk node, its invoices are leaves. Green = valid and trusted, red = the broken block, amber = individually fine but sitting *after* a broken block, so untrusted. Click a block or invoice for detail.

**Why a broken block stays broken:** blockchain trust is sequential. If block 1 is invalid, adding more valid blocks afterwards does not repair it, and every later block is unverifiable too. The system therefore keeps reporting the first broken block. The Chain Tree makes this visible.

---

## API Reference

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/` | Serves the dashboard |
| GET | `/api/state` | Full app state — businesses, nodes, chain, mempool, validity |
| POST | `/api/business/create` | Registers a business + wallet |
| POST | `/api/node/create` | Registers a validator node |
| POST | `/api/invoice/create` | Creates and seller-signs a new invoice |
| POST | `/api/invoice/acknowledge` | Buyer co-signs an existing invoice |
| POST | `/api/mine` | Mines all fully-signed pending invoices into a block |
| GET | `/api/validate` | Runs full chain validation across every node |
| POST | `/api/itc/claim` | Submits an ITC claim against a mined invoice |
| GET | `/api/fraud/check` | Scans the confirmed chain for fraud patterns |

---

## Sample Walkthrough

1. Register two businesses: "Acme Textiles" (Manufacturer) and "City Distributor" (Distributor). Copy both private keys.
2. Issue an invoice from Acme to City Distributor with two line items (e.g. Cotton Yarn at 5% GST, Packaging Boxes at 18% GST), signed with Acme's private key.
3. Switch to the acknowledge form and co-sign the same invoice with City Distributor's private key.
4. Mine the block — watch the proof-of-work nonce search happen.
5. Run the consensus check — both validator nodes should report the chain as valid.
6. Claim ITC as City Distributor against that invoice — it should be accepted, since it's genuine and mined.
7. Run the fraud scan — it should report no issues, since this is a legitimate transaction.
8. *(Optional, to see fraud detection in action)* Create three businesses and issue invoices A→B, B→C, C→A for the same goods, mine them all, then run the fraud scan again — circular trading should now be flagged.

---

## Limitations & Disclaimer

This is a **teaching simulation**, not production software:

- **No real cryptocurrency, GSTIN, or tax filing** is involved anywhere — GSTINs are randomly generated for the simulation, and "ITC" here is an in-memory number, not a real government-recognized credit.
- **In-memory / single-file JSON state** — there is no real database yet (see [Future Integration](#future-integration-and-possibilities)).
- **All validator nodes run in the same JVM process**, sharing one `Blockchain` object, rather than gossiping blocks over an actual network the way a real distributed blockchain would.
- **Private keys are shown in the browser UI** so the demo can sign transactions without a real wallet extension. A production system would never expose or transmit private keys this way.
- No transaction fees, mining rewards, or fork resolution — this models a single-writer chain with multiple independent *validators*, not a competing multi-miner network.

These are good, honest talking points for a viva if asked about production-readiness.

---

## Future Integration and Possibilities

- **Swap `Storage.java` for a real SQLite-backed database** (already scoped and tested to work via `libxerial-sqlite-jdbc-java` / JDBC) — full state survives a restart, and supports real SQL queries for auditing.
- **True multi-node networking** — run each `GSTNode` as its own separate process/machine, gossiping blocks over real sockets or HTTP, instead of sharing one in-memory `Blockchain`.
- **Real GSTIN validation** — integrate with the actual GSTN portal's public APIs to verify a business's real GSTIN instead of generating a simulated one.
- **E-way bill integration** — tie `Invoice` custody events to real E-way bill checkpoints for physical goods movement tracking.
- **Hardware-backed wallets** — move private key storage out of the browser entirely (e.g. a browser extension or hardware key), closing the biggest "not production-safe" gap called out above.
- **Machine-learning-assisted fraud detection** — layer a trained anomaly-detection model on top of `FraudDetector`'s rule-based checks (e.g. unusual invoice value spikes, timing patterns).
- **Mobile app / QR-based invoice verification** — let a buyer scan a QR code on a paper invoice to instantly check it against the chain.
- **Multi-language support** — Hindi and regional language support for the dashboard, given GST's nationwide reach.
- **Analytics dashboard** — visualizations of ITC claimed vs. invoiced over time, per-business risk scores, and network-wide fraud trend charts.
- **Formal digital signature integration** — tie signatures to a real identity system (e.g. Aadhaar-based e-Sign) instead of a simulation-only keypair.

---

*GST POTHI W3 — Genuine · Signed · Trustworthy.*
