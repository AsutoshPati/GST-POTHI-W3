# GST POTHI W3 — 5-Minute Demo Video Script

**Total runtime: ~5:00** | Format: screen recording + voiceover (or live narration)
**Before recording:** have the server running (`java -cp out PothiServer`), browser open to `http://localhost:8080`, and a text editor open with two blank private-key slots ready to paste into.

---

## 0:00 – 0:25 | Hook & Intro

**SAY:**
> "In the last five years, India's tax authorities detected over one lakh crore rupees in fake GST credit fraud — money claimed on invoices for goods that were never actually sold. I built a working simulation that shows how blockchain could make that kind of fraud structurally hard to commit in the first place. This is GST POTHI W3."

**DO:** Show the dashboard's masthead — the wordmark, the wax-seal status indicator, and the tagline "Genuine · Signed · Trustworthy." Let it sit on screen for 2–3 seconds before continuing.

---

## 0:25 – 1:00 | The Problem

**SAY:**
> "Right now, GST works on self-reporting. A seller reports an invoice, a buyer claims credit against it, and the two are matched later. The problem is shell companies — fake businesses that issue invoices for goods that never moved. Sometimes these invoices get passed in a loop — Company A sells to B, B to C, and C sells back to A — so a fake credit chain forms with nothing real ever changing hands. By the time authorities catch it with AI pattern detection, the fraud has already happened."

**DO:** No app interaction yet — this is voiceover only, optionally over a simple slide or just the paused dashboard.

---

## 1:00 – 1:30 | The Solution, in One Line

**SAY:**
> "GST POTHI flips the model. Instead of trusting a central authority to catch fraud after the fact, every invoice needs a real cryptographic signature from *both* the seller and the buyer before it counts. No second signature, no valid transaction. Let me show you."

**DO:** Click into the **Identities** tab. Point the cursor at the "Register a Business" form.

---

## 1:30 – 2:15 | Live Demo: Creating Identities

**SAY:**
> "First, I'll register two businesses. Each one gets a real cryptographic keypair — this is genuine ECDSA signing, the same kind of math used in real digital signatures — and a unique simulated GSTIN."

**DO:**
1. Type **"Acme Textiles"**, select **Manufacturer**, click **Issue Identity**.
2. Point out the success message: *"copy the private key shown — you'll need it to sign invoices."* Paste it into your text editor, labeled `SELLER_KEY`.
3. Type **"City Distributor"**, select **Distributor**, click **Issue Identity**. Paste that private key too, labeled `BUYER_KEY`.
4. Scroll to the **Registered Businesses** table on the right and point out both entries — name, role, simulated GSTIN, address.

**SAY (while scrolling):**
> "Notice each one has its own address, derived from its public key — this is the business's identity on the network, and it can never collide with another business's address."

---

## 2:15 – 3:15 | Live Demo: The Invoice Itself

**SAY:**
> "Now let's issue a real invoice. A GST invoice usually has multiple line items with different tax rates, so I built that in — this isn't a toy single-item model."

**DO:**
1. Switch to the **Invoices** tab.
2. Enter invoice number: **`INV/2026/0042`**.
3. Select Acme Textiles as **Seller**, City Distributor as **Buyer**.
4. Add first line item: **Cotton Yarn, 50, 200, 5** (name, qty, unit price, GST%).
5. Click **+ Add line item**, add second: **Packaging Boxes, 100, 30, 18**.
6. Paste `SELLER_KEY` into the private key box.
7. Click **Sign as Seller & Submit**.

**SAY:**
> "That invoice now exists — but it's not valid yet. It's sitting in the mempool waiting for the buyer to co-sign. Watch what happens if I try to skip that step."

**DO:** Point to the **Mempool** panel — show the invoice with the badge reading **"awaiting buyer."**

**SAY:**
> "Now the buyer confirms receipt with their own signature."

**DO:**
1. Scroll to **Acknowledge Receipt** form.
2. Select the pending invoice, select City Distributor as buyer, paste `BUYER_KEY`.
3. Click **Co-sign as Buyer**.
4. Point to the mempool badge flipping to **"fully signed."**

---

## 3:15 – 3:50 | Live Demo: Mining and Consensus

**SAY:**
> "Now I'll mine this into a block — this does real proof-of-work, the same core idea Bitcoin uses, just at a lower difficulty so it doesn't take forever on camera."

**DO:**
1. Switch to **Mine & Validate** tab, select a validator node, click **Mine Block**.
2. Point to the resulting certificate card — block number, hash, nonce.
3. Click **Validate Chain**.

**SAY:**
> "Every validator node just independently re-checked the entire chain from scratch — every hash, every proof-of-work, every signature — and they all agree it's genuine. No central server had to be trusted."

**DO:** Point to both nodes reporting **VALID**.

---

## 3:50 – 4:35 | The "Wow" Moment: Catching Fraud

**SAY:**
> "Now let me show you what this looks like when fraud is actually attempted. I'll simulate the circular trading pattern I mentioned earlier — three businesses passing the same goods in a loop."

**DO:** *(If time allows, pre-stage this before recording so it's quick — three businesses A, B, C, with invoices A→B, B→C, C→A already signed and mined.)*

1. Switch to **ITC & Fraud** tab.
2. Click **Scan Confirmed Chain**.
3. Let the red fraud alert render on screen: *"Circular trading pattern detected."*

**SAY:**
> "The system just caught it automatically — goods that supposedly moved from A to B to C somehow ended up back at A. In the real world, this exact pattern is one of the most common ways fake Input Tax Credit gets manufactured. Here, it's flagged instantly, before any credit is even claimed."

---

## 4:35 – 4:50 | Under the Hood (Quick Credibility Beat)

**SAY:**
> "Under the hood, this is fifteen Java classes, each one independently runnable and testable — real SHA-256 hashing, real ECDSA signatures, two inheritance hierarchies for the tamper-evident objects and the network identities, and zero external libraries. Everything you just saw is backed by actual working code, not a mockup."

**DO:** Optionally flash the project's file structure or one class file (e.g. `Invoice.java`) briefly on screen.

---

## 4:50 – 5:00 | Closing

**SAY:**
> "GST POTHI W3 — genuine, signed, trustworthy. Instead of asking people to trust the authority, it asks them to verify the evidence. Thank you."

**DO:** End on the dashboard masthead with the wax seal showing **"VERIFIED GENUINE."**

---

## Tips for Recording

- **Pre-stage the fraud demo** (Section 4) before hitting record — building it live eats too much time and risks fumbling the form.
- **Keep private keys in a side text file**, not memorized — copy-pasting on camera looks cleaner than typing them.
- **Speak slightly slower than feels natural** — screen recordings always feel rushed on playback.
- If you go over 5 minutes, the safest cuts are: shortening the Problem section (0:25–1:00) to one sentence, or trimming the "Under the Hood" beat entirely.
