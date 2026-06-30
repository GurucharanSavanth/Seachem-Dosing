package com.example.seachem_dosing.domain.medication

/**
 * Evidence-grounded medication catalog (SPEC §C: no invented facts). Source
 * grades are maintained in PRODUCT_CATALOG_SCHEMA.md: Seachem entries are
 * OFFICIAL, the others SECONDARY pending task #14.
 * Anything not on an authoritative source is left null/UNKNOWN, never guessed.
 *
 * In-code reference data (small, stable) like the fertilizer chemistry table.
 * The richer searchable catalog moves to Room/JSON seed in Phase 8.
 */
enum class WaterType { FRESHWATER, SALTWATER, BRACKISH }

/** Reef safety is never asserted "safe" without manufacturer evidence (§V4). */
enum class ReefSafety { NOT_SAFE, UNKNOWN }

enum class Evidence { OFFICIAL, SECONDARY }

data class MedProduct(
    val id: String,
    val brand: String,
    val name: String,
    val actives: List<String>,
    val category: String,
    /** Water types the label verifies. Unknown ≠ included (§V4). */
    val waterTypes: Set<WaterType>,
    val route: String,
    val doseRule: String,
    val duration: String,
    val removeChemFiltration: Boolean,
    val removeInverts: Boolean,
    val reefSafe: ReefSafety,
    /** Antibiotic / copper / formaldehyde / malachite-green → stricter gating (§V5). */
    val highRisk: Boolean,
    val evidence: Evidence,
    /** False when the label dose could not be confirmed from the manufacturer page (e.g. Kordon, #14). */
    val doseVerified: Boolean = true,
)

object MedicationCatalog {

    private val FW_SW = setOf(WaterType.FRESHWATER, WaterType.SALTWATER)
    private val FW = setOf(WaterType.FRESHWATER)

    val ALL: List<MedProduct> = listOf(
        // ---- Seachem (OFFICIAL) ----
        MedProduct("seachem_kanaplex", "Seachem", "KanaPlex", listOf("kanamycin"), "antibacterial+antifungal",
            FW_SW, "water column; medicated food", "1 measure per 20 L (5 US gal)", "every 48 h, max 3 doses",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_metroplex", "Seachem", "MetroPlex", listOf("metronidazole"), "antiparasitic+antibacterial",
            FW_SW, "water column; medicated food", "1–2 measures per 40 L (10 US gal)", "every 48 h, up to 3 weeks",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_neoplex", "Seachem", "NeoPlex", listOf("neomycin sulfate"), "antibacterial (broad)",
            FW_SW, "water column; medicated food", "1 measure per 8 L (2 US gal)", "every 7 d, max 3 weeks",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_sulfaplex", "Seachem", "SulfaPlex", listOf("sulfathiazole"), "antibacterial+antifungal",
            FW_SW, "water column; medicated food", "2–3 measures per 40 L (10 US gal)", "every 72 h, max 3 weeks",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_polyguard", "Seachem", "PolyGuard",
            listOf("sulfathiazole", "malachite green", "nitrofurantoin", "nitrofural", "quinacrine dihydrochloride"),
            "broad spectrum (bacterial/fungal/parasitic)", FW_SW, "water column", "1 measure per 40 L (10 US gal)", "every 72 h, max 2 weeks",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_paraguard", "Seachem", "ParaGuard", listOf("aldehydes", "malachite green"), "antiparasitic+antifungal+antibacterial",
            FW_SW, "water column; dip", "5 mL (1 capful) per 40 L (10 US gal) daily", "ich 14 d FW / 28 d SW; varies by condition",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.NOT_SAFE, highRisk = true, evidence = Evidence.OFFICIAL),
        MedProduct("seachem_cupramine", "Seachem", "Cupramine", listOf("copper (amine complex)"), "antiparasitic (copper)",
            FW_SW, "water column", "1 mL per 40 L day 1, repeat 48 h; half dose in FW (target 0.5 mg/L SW, 0.25 mg/L FW)", "14 days; do not redose without testing copper",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.NOT_SAFE, highRisk = true, evidence = Evidence.OFFICIAL),

        // ---- API / Fritz / Kordon (SECONDARY — verify task #14) ----
        MedProduct("api_general_cure", "API", "General Cure", listOf("metronidazole", "praziquantel"), "antiparasitic",
            FW, "water column", "1 packet per 10 US gal (38 L), repeat after 48 h", "per label",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.SECONDARY),
        MedProduct("api_em_erythromycin", "API", "E.M. Erythromycin", listOf("erythromycin"), "antibacterial (broad)",
            FW, "water column", "1 packet per 10 US gal, repeat after 24 h", "per label",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.SECONDARY),
        MedProduct("fritz_maracyn", "Fritz", "Maracyn", listOf("erythromycin"), "antibacterial",
            FW_SW, "water column", "Day 1: 2 packets / 10 gal; Day 2–5: 1 packet / 10 gal; Day 6: 25% water change", "5-day course",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.SECONDARY),
        MedProduct("fritz_maracyn_two", "Fritz", "Maracyn-Two", listOf("minocycline"), "antibacterial",
            FW_SW, "water column", "Day 1: 2 packets / 10 gal; Day 2–5: 1 packet / 10 gal; Day 6: 25% water change", "5-day course",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.SECONDARY),
        MedProduct("kordon_rid_ich_plus", "Kordon", "Rid-Ich Plus", listOf("formaldehyde", "malachite green"), "antiparasitic (ich)",
            FW_SW, "water column", "AQUARIUM DOSE UNVERIFIED — confirm from label (task #14)", "per label",
            removeChemFiltration = true, removeInverts = true, reefSafe = ReefSafety.UNKNOWN, highRisk = true, evidence = Evidence.SECONDARY, doseVerified = false),
    )

    fun byId(id: String): MedProduct? = ALL.firstOrNull { it.id == id }

    /** Products whose label verifies [waterType] (§V4: unknown compatibility excluded). */
    fun forWaterType(waterType: WaterType): List<MedProduct> = ALL.filter { waterType in it.waterTypes }
}
