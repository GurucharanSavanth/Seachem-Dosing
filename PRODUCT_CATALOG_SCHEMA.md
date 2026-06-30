# PRODUCT_CATALOG_SCHEMA.md

**Project:** Seachem-Dosing v2.0 · **Updated:** 2026-06-28

Schemas for the in-code reference catalogs. (Phase 8 keeps these in Kotlin objects — small, stable, version-controlled. A Room/JSON seed is the upgrade path if the catalog grows or needs user editing.)

## Medication — `MedProduct` (`domain/medication/MedicationCatalog.kt`)

| field | type | required | allowed / notes |
|---|---|---|---|
| id | String | yes | unique (§V8); `{brand}_{name}` lowercase snake |
| brand | String | yes | e.g. Seachem, API, Fritz, Kordon |
| name | String | yes | product name |
| actives | List<String> | yes | active ingredient(s); from SDS/label |
| category | String | yes | antibacterial / antiparasitic / antifungal / broad / copper … |
| waterTypes | Set<WaterType> | yes, non-empty | {FRESHWATER, SALTWATER, BRACKISH} — **only label-verified types** (§V4: unknown excluded) |
| route | String | yes | water column / medicated food / dip |
| doseRule | String | yes | verbatim label dose |
| duration | String | yes | treatment duration / re-dose |
| removeChemFiltration | Boolean | yes | carbon/UV/ozone removal required |
| removeInverts | Boolean | yes | remove invertebrates |
| reefSafe | enum | yes | NOT_SAFE \| UNKNOWN (never "SAFE" without evidence, §V4) |
| highRisk | Boolean | yes | antibiotic/copper/formaldehyde/malachite → §V5 gating |
| evidence | enum | yes | OFFICIAL \| SECONDARY |
| doseVerified | Boolean | default true | false ⇒ surface "verify label" warning (e.g. Kordon, #14) |

**Validation:** `MedicationSearchEngine.duplicateIds()` must be empty (§V8); `waterTypes` non-empty; `actives` non-empty; OFFICIAL entries map to a manufacturer source below.

### Medication Evidence Sources

| product ids | grade | source |
|---|---|---|
| `seachem_kanaplex`, `seachem_metroplex`, `seachem_neoplex`, `seachem_sulfaplex`, `seachem_polyguard`, `seachem_paraguard`, `seachem_cupramine` | OFFICIAL | Seachem manufacturer product pages, `https://www.seachem.com/` |
| `api_general_cure`, `api_em_erythromycin` | SECONDARY | API manufacturer product pages, `https://www.apifishcare.com/` |
| `fritz_maracyn`, `fritz_maracyn_two` | SECONDARY | Fritz/Mardel manufacturer product pages, `https://fritzaquatics.com/` |
| `kordon_rid_ich_plus` | SECONDARY, `doseVerified=false` | Kordon manufacturer product page, `https://www.kordon.com/` |

## Fertilizer — `Compound` (`domain/engine/FertilizerChemistryEngine.kt`)

| field | type | notes |
|---|---|---|
| id | String | unique |
| formula | String | e.g. `KNO3`, `MgSO4·7H2O` |
| molarMass | BigDecimal | g/mol, from IUPAC standard atomic weights (WS3, arithmetic shown) |
| nutrients | List<Nutrient(symbol, massFraction)> | massFraction ∈ (0,1] |
| category | enum | MACRO \| MICRO \| GH_KH \| REEF |

**Validation:** Σ no constraint (compounds carry only the nutrients of interest); massFraction derived = element mass / molarMass using IUPAC conventional atomic weights; ppm identity `dose_g × fraction × 1000 / volume_L` unit-tested.

## Symptom (beginner observation set, `ui/medication/MedicationScreen.kt`)

Plain observation labels (not diagnoses). Never mapped to a drug without cited evidence — beginner flow shows water-type-filtered, safety-gated candidates + "not a diagnosis / consult expert" (§G, fail-safe).

## Evidence grading (shared)

`OFFICIAL` = manufacturer page/label/SDS (fetched). `SECONDARY` = search summary of an official page (pending direct fetch, task #14). `STANDARD` = IUPAC/definitional. `UNKNOWN` = not on an authoritative source — stored as null/UNKNOWN, never invented.
