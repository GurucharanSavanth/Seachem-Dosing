package com.example.seachem_dosing.ui.calculators

import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.logic.SeachemCalculations.CalculationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** ADR-011 §11 — calculator dose logging uses the physical g/mL amount, never the display spoon. */
class DoseLoggingTest {

    private fun result(pv: String, pu: String, sv: String, su: String) =
        CalculationResult(BigDecimal(pv), pu, BigDecimal(sv), su)

    @Test fun primaryGrams_areUsed() {
        val req = doseLogRequestFrom("FLOURISH", result("5.0", "g", "1.0", "tsp"), 100.0)!!
        assertEquals("FLOURISH", req.productId)
        assertEquals(UnitCode.GRAM, req.unit)
        assertEquals("5.0", req.amount.canonicalValue)
        assertEquals(100.0, req.volumeLitres, 0.0)
    }

    @Test fun primaryMl_areUsed() {
        val req = doseLogRequestFrom("PRIME", result("3.0", "mL", "0.6", "caps"), 50.0)!!
        assertEquals(UnitCode.MILLILITER, req.unit)
        assertEquals("3.0", req.amount.canonicalValue)
    }

    @Test fun whenPrimaryIsASpoon_secondaryGramsAreUsed() {
        val req = doseLogRequestFrom("KHCO3", result("2.0", "tsp", "10.0", "g"), 100.0)!!
        assertEquals(UnitCode.GRAM, req.unit)
        assertEquals("10.0", req.amount.canonicalValue) // never logs the product-specific spoon
    }

    @Test fun noPhysicalUnit_returnsNull() {
        assertNull(doseLogRequestFrom("X", result("1.0", "tsp", "2.0", "caps"), 100.0))
    }

    @Test fun zeroAmount_returnsNull() {
        assertNull(doseLogRequestFrom("X", result("0", "g", "0", "tsp"), 100.0))
    }
}
