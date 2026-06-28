package com.example.seachem_dosing.domain.medication

/**
 * Search + indexing over [MedicationCatalog] (SPEC §V4, §V8).
 *
 * §V4 — results filter by water type by default; a product is returned for a
 * water type ONLY if its label verifies that type (unknown ≠ compatible).
 * §V8 — [duplicateIds] guards the catalog against duplicate product IDs.
 */
object MedicationSearchEngine {

    private data class Indexed(val product: MedProduct, val tokens: String)

    private val INDEX: List<Indexed> = MedicationCatalog.ALL.map {
        Indexed(it, buildString {
            append(it.name).append(' ').append(it.brand).append(' ')
            append(it.actives.joinToString(" ")).append(' ').append(it.category)
        }.lowercase())
    }

    /**
     * Products matching [query], filtered to [waterType] when given (§V4).
     * Blank query returns all (water-type-filtered). Name-prefix matches rank first.
     */
    fun search(query: String, waterType: WaterType? = null): List<MedProduct> {
        val q = query.trim().lowercase()
        return INDEX.asSequence()
            .filter { waterType == null || waterType in it.product.waterTypes }
            .filter { q.isEmpty() || it.tokens.contains(q) }
            .sortedByDescending { it.product.name.lowercase().startsWith(q) }
            .map { it.product }
            .toList()
    }

    /** Any duplicate product IDs in the catalog (§V8 — must be empty). */
    fun duplicateIds(): List<String> =
        MedicationCatalog.ALL.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.toList()
}
