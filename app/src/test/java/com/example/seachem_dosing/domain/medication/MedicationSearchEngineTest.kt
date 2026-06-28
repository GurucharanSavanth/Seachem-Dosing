package com.example.seachem_dosing.domain.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Catalog search/separation invariants (SPEC §V4, §V8). */
class MedicationSearchEngineTest {

    @Test
    fun search_by_name_finds_product() {
        val r = MedicationSearchEngine.search("kana")
        assertTrue(r.any { it.id == "seachem_kanaplex" })
    }

    @Test
    fun search_by_active_ingredient() {
        assertTrue(MedicationSearchEngine.search("copper").any { it.id == "seachem_cupramine" })
    }

    @Test
    fun saltwater_filter_excludes_freshwater_only_product() {  // §V4
        val sw = MedicationSearchEngine.search("", WaterType.SALTWATER)
        assertTrue(sw.none { it.id == "api_em_erythromycin" }) // API E.M. is freshwater-only in the catalog
    }

    @Test
    fun freshwater_filter_includes_freshwater_product() {
        val fw = MedicationSearchEngine.search("", WaterType.FRESHWATER)
        assertTrue(fw.any { it.id == "api_em_erythromycin" })
    }

    @Test
    fun no_duplicate_ids() {  // §V8
        assertEquals(emptyList<String>(), MedicationSearchEngine.duplicateIds())
    }

    @Test
    fun blank_query_returns_all_unfiltered() {
        assertEquals(MedicationCatalog.ALL.size, MedicationSearchEngine.search("").size)
    }
}
