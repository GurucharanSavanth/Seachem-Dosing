package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.repository.CalculationsRepository
import com.example.seachem_dosing.domain.model.DosingResult
import com.example.seachem_dosing.logic.SeachemCalculations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateDoseUseCaseTest {

    private val calculationsRepository: CalculationsRepository = mockk(relaxed = true)
    private val useCase = CalculateDoseUseCase(calculationsRepository)

    @Test
    fun `returns Error when volume is zero`() {
        val result = useCase(
            product = SeachemCalculations.Product.EQUILIBRIUM,
            currentValue = BigDecimal("4.0"),
            targetValue = BigDecimal("6.0"),
            volumeLitres = BigDecimal.ZERO,
            scale = SeachemCalculations.UnitScale.DKH
        )

        assertTrue("expected Error, got $result", result is DosingResult.Error)
        assertEquals("ZERO_VOLUME", (result as DosingResult.Error).code)
        assertTrue(result.recoverable)
    }

    @Test
    fun `returns Error when volume is negative`() {
        val result = useCase(
            product = SeachemCalculations.Product.EQUILIBRIUM,
            currentValue = BigDecimal("4.0"),
            targetValue = BigDecimal("6.0"),
            volumeLitres = BigDecimal("-1.0"),
            scale = SeachemCalculations.UnitScale.DKH
        )

        assertTrue("expected Error, got $result", result is DosingResult.Error)
    }

    @Test
    fun `delegates to repository for valid inputs`() {
        val expected = DosingResult.Success(
            primaryValue = BigDecimal("13.333"),
            primaryUnit = "g",
            secondaryValue = BigDecimal("0.833"),
            secondaryUnit = "tbsp"
        )
        every {
            calculationsRepository.calculateForProduct(
                product = SeachemCalculations.Product.EQUILIBRIUM,
                currentValue = BigDecimal("4.0"),
                targetValue = BigDecimal("6.0"),
                volumeLitres = BigDecimal("100.0"),
                scale = SeachemCalculations.UnitScale.DKH
            )
        } returns expected

        val result = useCase(
            product = SeachemCalculations.Product.EQUILIBRIUM,
            currentValue = BigDecimal("4.0"),
            targetValue = BigDecimal("6.0"),
            volumeLitres = BigDecimal("100.0"),
            scale = SeachemCalculations.UnitScale.DKH
        )

        assertEquals(expected, result)
        verify(exactly = 1) {
            calculationsRepository.calculateForProduct(
                product = SeachemCalculations.Product.EQUILIBRIUM,
                currentValue = BigDecimal("4.0"),
                targetValue = BigDecimal("6.0"),
                volumeLitres = BigDecimal("100.0"),
                scale = SeachemCalculations.UnitScale.DKH
            )
        }
    }

    @Test
    fun `does not invoke repository when volume invalid`() {
        useCase(
            product = SeachemCalculations.Product.EQUILIBRIUM,
            currentValue = BigDecimal("4.0"),
            targetValue = BigDecimal("6.0"),
            volumeLitres = BigDecimal.ZERO,
            scale = SeachemCalculations.UnitScale.DKH
        )

        verify(exactly = 0) {
            calculationsRepository.calculateForProduct(any(), any(), any(), any(), any())
        }
    }
}
