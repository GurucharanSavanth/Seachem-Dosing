# DEEP_RESEARCH_REPORT.md

**Project:** Seachem-Dosing → aquarium super-app v2.0
**Date:** 2026-06-27
**Method:** main-thread WebSearch/WebFetch against primary sources, evidence-graded. Async research agents were unreliable (see TOOL_FAILURE_LOG F-05); WebFetch was rate-limited mid-run (F-06) — items needing a verbatim page fetch are marked **SECONDARY (verify)** and tracked as pending.

**Grade key:** `OFFICIAL` = stated on the manufacturer's own product page (fetched). `STANDARD` = derivable from IUPAC standard atomic weights / definitional identity (arithmetic shown — independently checkable). `SECONDARY` = from a search-result summary of an official page, not yet confirmed by direct page fetch. `UNKNOWN` = not found on an authoritative source. No dose, ingredient, formula, or version below is invented.

---

## TL;DR

1. **minSdk 33 is viable and reaches ~68.9% of active Android devices** (API 34+ = 54.5%; Statcounter Apr 2026). All toolchain libraries support a floor far below 33.
2. **Two API-level behaviors actually bite this app** at targetSdk 36: **edge-to-edge is enforced (API 35+)** → the Compose migration must handle window insets; and **POST_NOTIFICATIONS is a runtime permission (API 33)** → required only if/when the app posts reminders/alerts. Foreground-service-types (API 34) do **not** apply (no FGS in this app).
3. **Medication evidence:** 7 Seachem products fully sourced from manufacturer pages (active ingredient, dose, carbon/UV-removal, invertebrate/reef warnings). 5 non-Seachem products (API ×2, Fritz ×2, Kordon ×1) gathered at SECONDARY grade — verbatim doses pending a page re-fetch.
4. **A recurring manufacturer safety pattern is verified across all 7 Seachem meds:** *remove invertebrates, turn off UV/ozone, remove chemical filtration (carbon/Purigen) during dosing.* This becomes a hard rule in the medication engine.
5. **Fertilizer chemistry:** molar masses + nutrient mass-fractions + ppm derivation computed for 8 DIY compounds (arithmetic shown). Solubility values + CSM+B guaranteed-analysis pending web.
6. **Two contradictions surfaced and are NOT silently resolved:** NeoPlex's page carries two opposing invertebrate statements; the Kordon search snippet conflated *Pond* Rid-Ich Plus dosing with the aquarium product. Both flagged, not guessed.

---

## WS1 — Android modernization (minSdk 24 → 33, targetSdk/compileSdk 36)

| Topic | Finding | Grade |
|---|---|---|
| POST_NOTIFICATIONS | Runtime permission introduced **API 33 (Android 13)**. Apps must declare it + request at runtime before posting notifications. At minSdk 33 the permission applies on every supported device. | OFFICIAL ([developer.android.com](https://developer.android.com/develop/ui/views/notifications/notification-permission)) |
| FGS types | Apps targeting **API 34 (Android 14)** must declare `foregroundServiceType`. **A utility/calculator app with no foreground services is unaffected.** | OFFICIAL ([fgs-types-required](https://developer.android.com/about/versions/14/changes/fgs-types-required)) |
| Edge-to-edge | **Enforced for apps targeting API 35+.** Status/nav bars transparent; content draws behind them; `setStatusBarColor`/`setNavigationBarColor` deprecated; `Configuration.screenWidthDp/heightDp` no longer exclude system bars. **Must handle `WindowInsets`** — Material 3 `Scaffold` does this automatically. | OFFICIAL ([behavior-changes-15](https://developer.android.com/about/versions/15/behavior-changes-15)) |
| Device coverage | **API 33+ = 68.9%**, API 34+ = 54.5% of active devices (Statcounter Apr 2026, via apilevels.com). | SECONDARY ([apilevels.com](https://apilevels.com/)) |
| Library floors | Compose BOM 2024.12.01, Koin 4.0.0, Room 2.6.1, navigation-compose 2.8.5, activity-compose 1.9.3, lifecycle 2.8.7, datastore 1.0.0, Material 1.11.0 — all support minSdk far below 33. None requires > 33. | STANDARD (declared floors ≤ 24) |

**Implications for the plan:**
- minSdk 33 is a deliberate ~31% device-coverage trade for a cleaner modern baseline — **document in an ADR** (owner explicitly chose "33 and up" for latest-SDK features).
- `Build.VERSION.SDK_INT < 33` guards become dead code → remove during Phase 1.
- The Compose migration (Phase 4-5) **must** apply insets (edge-to-edge) — a real, sourced requirement, not a nicety.
- POST_NOTIFICATIONS only matters when the medication/reminder feature posts notifications; gate that work behind the runtime request.

---

## WS2 — Medication catalog evidence

> **Safety framing (governs the whole module):** this is **evidence-grounded decision support, not veterinary diagnosis.** Every dose below is the manufacturer's label figure, cited. Where a field is not on an official source it is **UNKNOWN** — the app must not synthesise it. High-risk actives (antibiotics, copper, formaldehyde, malachite green) require confirmed species/water-type/volume/inverts/filtration before any dose is shown.

### Seachem (7/7 — OFFICIAL, manufacturer product pages, accessed 2026-06-27, cross-checked by main thread + subagent)

| Product | Active (per page) | Category | Water | Route | Label dose | Duration / re-dose | Remove carbon/UV/ozone? | Invert / reef |
|---|---|---|---|---|---|---|---|---|
| **KanaPlex** | kanamycin-based | antibacterial + antifungal | FW + marine | water column; medicated food | 1 measure / 20 L (5 gal) | every 48 h, max 3 doses | **Yes** (UV/ozone off; remove carbon/Purigen) | remove all inverts; reef UNKNOWN |
| **MetroPlex** | metronidazole 70% | antiparasitic + anaerobic antibacterial | FW + marine | water column; food | 1–2 measures / 40 L (10 gal) | every 48 h, up to 3 wk | **Yes** | remove inverts; "would recommend in medicated food" but not 100% reef-safe |
| **NeoPlex** | neomycin sulfate 43% | broad-spectrum antibacterial | FW + marine | water column; food | 1 measure / 8 L (2 gal) | every 7 d, max 3 wk | **Yes** | ⚠️ page states BOTH "well tolerated by invertebrates" AND "remove all invertebrates" — contradiction, see §Rejected |
| **SulfaPlex** | sulfathiazole 69% | antibacterial + antifungal | FW + SW | water column; food | 2–3 measures / 40 L (10 gal) | every 72 h, max 3 wk | **Yes** | remove inverts; reef UNKNOWN |
| **PolyGuard** | sulfathiazole 36%, malachite green 1.9%, nitrofurantoin 0.14%, nitrofural 0.14%, quinacrine·2HCl 0.27% | broad-spectrum (bact/fungal/parasitic) | FW + marine | water column | 1 measure / 40 L (10 gal) | every 72 h, max 2 wk | **Yes** | remove inverts; reef UNKNOWN |
| **ParaGuard** | aldehyde-based 10% w/w + malachite green + protective polymers | ectoparasites, fungal/bacterial/viral lesions | FW + SW | water column; **dip** | 5 mL (1 cap) / 40 L (10 gal) daily; dip 3 mL / 4 L | ich 14 d FW / 28 d SW; velvet 14-21 d; fin rot 7 d; flukes 14 d | remove inverts + chemical filtration (Purigen) | **Not for reef**; eels/loaches/rays/sharks → start partial dose |
| **Cupramine** | copper complexed to organic amine | antiparasitic (copper) | FW + marine | water column | 1 mL / 40 L day 1, repeat 48 h; **half dose in FW**; target 0.5 mg/L SW, 0.25 mg/L FW | 14 d at full conc; **do not redose without testing Cu** | **Yes** | remove inverts (Cu-sensitive); **not reef-safe** |

CAS numbers / SDS Section-3 composition = **UNKNOWN** (Seachem SDS sits behind a FileMaker DB + Dropbox PDFs, not machine-retrievable). Required during-treatment water-change protocol = **UNKNOWN** for all 7 (not on the pages).

### API / Fritz / Kordon (5 — SECONDARY, verify verbatim doses; pending F-06)

| Product | Active (reported) | Category | Water | Label dose (reported) | Carbon | Grade / note |
|---|---|---|---|---|---|---|
| **API General Cure** | metronidazole + praziquantel | antiparasitic (velvet, lice, Hexamita/Spironucleus, gill/skin flukes) | FW (marine UNKNOWN) | 1 packet / 10 gal (38 L), repeat after 48 h | remove carbon, keep aeration | SECONDARY ([apifishcare.com](https://www.apifishcare.com/products/api-general-cure-powder-packets); SDS exists) |
| **API E.M. Erythromycin** | erythromycin | broad-spectrum antibacterial (body slime, mouth fungus, furunculosis, gill disease, hemorrhagic septicemia) | FW (marine UNKNOWN) | 1 packet / 10 gal, repeat after 24 h | leave carbon out entire treatment | SECONDARY ([apifishcare.com](https://www.apifishcare.com/products/api-em-erythromycin-powder-packets)) |
| **Fritz Maracyn** | erythromycin (Mardel) | antibacterial | FW + SW (same dose) | Day 1: 2 packets / 10 gal; Day 2-5: 1 packet / 10 gal; Day 6: 25% water change | remove carbon, keep aeration; no water changes during course | SECONDARY ([fritzaquatics.com](https://fritzaquatics.com/products/mardel-maracyn); SDS 042319) |
| **Fritz Maracyn-Two** | minocycline (Mardel) | antibacterial | FW + SW | same 5-day schedule (Day 1 double, Day 2-5 single, Day 6 25% WC) | remove carbon | SECONDARY ([fritzaquatics.com](https://fritzaquatics.com/products/mardel-maracyn-2); SDS 042519) |
| **Kordon Rid-Ich Plus** | formaldehyde (4.26% USP) + zinc-free malachite green chloride (0.038%) | external protozoan/dinoflagellate/sporozoan parasites (ich) | FW + marine | ⚠️ **aquarium dose NOT verified** — search summary returned the *Pond* product dose (1 cap / 8 gal). Active ingredients from aquarium page. | remove carbon; 25%+ water change before each treatment | SECONDARY ([kordon.com](https://www.kordon.com/kordon/products/chemical-preventatives-and-treatments-2/rid-ich)) — **verify aquarium dose** |

---

## WS3 — DIY fertilizer chemistry (computed; STANDARD)

**Standard atomic weights used (IUPAC conventional, g/mol):** H 1.008, C 12.011, N 14.007, O 15.999, Na 22.990, Mg 24.305, P 30.974, S 32.06, Cl 35.45, K 39.098, Ca 40.078; H₂O 18.015.

**ppm identity (definitional):** `ppm(mg/L) = dose_g × nutrient_mass_fraction × 1000 / volume_L`.

| Compound | Molar mass (arithmetic) | Nutrient fractions | Worked ppm (1 g in 100 L) |
|---|---|---|---|
| **KNO₃** | 39.098 + 14.007 + 3·15.999 = **101.102** | N 14.007/101.102 = **13.85%**; K **38.67%**; NO₃ 61.33% | N 1.385, K 3.867 mg/L |
| **KH₂PO₄** | 39.098 + 2·1.008 + 30.974 + 4·15.999 = **136.084** | P 30.974/136.084 = **22.76%**; K **28.73%**; (P₂O₅ eq 52.15%) | P 2.276, K 2.873 mg/L |
| **K₂SO₄** | 2·39.098 + 32.06 + 4·15.999 = **174.252** | K 78.196/174.252 = **44.87%**; S 18.40% | K 4.487 mg/L |
| **MgSO₄·7H₂O** | 120.361 + 7·18.015 = **246.466** | Mg 24.305/246.466 = **9.86%**; S 13.01% | Mg 0.986 mg/L |
| **CaSO₄·2H₂O** | 40.078 + 32.06 + 4·15.999 + 2·18.015 = **172.164** | Ca 40.078/172.164 = **23.28%**; S 18.62% | Ca 2.328 mg/L |
| **CaCl₂ / ·2H₂O** | 110.978 / **147.008** | Ca **36.11%** (anhyd) / **27.26%** (dihydrate) | Ca 3.611 / 2.726 mg/L |
| **NaHCO₃** | 22.990 + 1.008 + 12.011 + 3·15.999 = **84.006** | HCO₃ 72.63%; Na 27.37% | alkalinity: ~84 mg/L ≈ **+1 meq/L ≈ +2.8 dKH** |
| **MgCl₂·6H₂O** | 24.305 + 2·35.45 + 6·18.015 = **203.295** | Mg 24.305/203.295 = **11.96%** | Mg 1.196 mg/L |
| **CSM+B** | proprietary trace blend | per-manufacturer guaranteed analysis | **UNKNOWN — verify from label** (do not hardcode Fe/Mn/etc. from memory) |

**Macro/micro separation:** the chemical basis is verifiable — Fe³⁺ + PO₄³⁻ → **FePO₄** (sparingly soluble), so concentrated phosphate + iron/trace stock solutions precipitate and concentrated mixes invite microbial growth. The engine **defaults to separate macro (N,P,K) and micro (trace/Fe) stocks.** (Mechanism = STANDARD chemistry; a manufacturer/extension citation for the operational rule is pending web — F-06.)

---

## Confidence & gaps

- **High:** WS1 Android behaviors (official); Seachem actives + doses + filtration/invert warnings (official, double-sourced); WS3 molar masses + fractions (definitional arithmetic).
- **Medium:** device-coverage % (reputable aggregator, not Google direct); non-Seachem actives (consistent but search-summary).
- **Low / pending:** non-Seachem verbatim doses (esp. Kordon aquarium); all SDS/CAS; solubility values; CSM+B analysis; during-treatment water-change protocols.

## Rejected / contradictory claims (surfaced, not resolved by guessing)

1. **NeoPlex invertebrate safety** — the official page contains both "well tolerated by invertebrates, although delicate species may be stressed" and "Remove all invertebrates – these are extremely sensitive to medication." The app will present the **conservative** instruction (remove inverts) and flag the conflict, not silently pick one.
2. **Kordon Rid-Ich Plus dose** — a search summary returned *Pond* Rid-Ich Plus dosing for the aquarium product. Aquarium dose is treated as **UNVERIFIED** until the aquarium page/label is fetched.

## Pending web-verification (task #14, after ~17:50 IST)

- Re-fetch the 5 non-Seachem product pages + SDS for verbatim dose/active confirmation → upgrade SECONDARY→OFFICIAL.
- Fetch PubChem solubility for the 8 fertilizer compounds; fetch CSM+B guaranteed analysis from the manufacturer label.
- Optionally recover the API/Fritz/Kordon subagent's 34-tool-use findings.

## Source ledger

1. https://developer.android.com/develop/ui/views/notifications/notification-permission — Google — 2026-06-27 — OFFICIAL
2. https://developer.android.com/about/versions/14/changes/fgs-types-required — Google — 2026-06-27 — OFFICIAL
3. https://developer.android.com/about/versions/15/behavior-changes-15 — Google — 2026-06-27 — OFFICIAL
4. https://apilevels.com/ — apilevels.com (cites Statcounter) — 2026-06-27 — SECONDARY
5–11. https://www.seachem.com/{kanaplex,metroplex,neoplex,sulfaplex,polyguard,paraguard,cupramine}.php — Seachem — 2026-06-27 — OFFICIAL
12. https://www.apifishcare.com/products/api-general-cure-powder-packets — API/Mars Fishcare — 2026-06-27 — SECONDARY
13. https://www.apifishcare.com/products/api-em-erythromycin-powder-packets — API/Mars Fishcare — 2026-06-27 — SECONDARY
14. https://fritzaquatics.com/products/mardel-maracyn — Fritz Aquatics — 2026-06-27 — SECONDARY
15. https://fritzaquatics.com/products/mardel-maracyn-2 — Fritz Aquatics — 2026-06-27 — SECONDARY
16. https://www.kordon.com/kordon/products/chemical-preventatives-and-treatments-2/rid-ich — Kordon/Novalek — 2026-06-27 — SECONDARY
17. Fertilizer molar masses — IUPAC standard atomic weights (definitional) — STANDARD

_Forums, Reddit, blogs, YouTube, and retailer reviews were excluded as proof per the evidence hierarchy._
