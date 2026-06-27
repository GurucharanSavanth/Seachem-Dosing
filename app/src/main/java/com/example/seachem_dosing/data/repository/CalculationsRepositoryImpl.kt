package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.domain.model.DosingResult
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.logic.SeachemCalculations
import java.math.BigDecimal

class CalculationsRepositoryImpl : CalculationsRepository {

    override fun calculateForProduct(
        product: SeachemCalculations.Product,
        currentValue: BigDecimal,
        targetValue: BigDecimal,
        volumeLitres: BigDecimal,
        scale: SeachemCalculations.UnitScale
    ): DosingResult {
        val raw = dispatch(product, currentValue, targetValue, volumeLitres, scale)
        return DosingResult.Success(
            primaryValue = raw.primaryValue,
            primaryUnit = raw.primaryUnit,
            secondaryValue = raw.secondaryValue,
            secondaryUnit = raw.secondaryUnit
        )
    }

    private fun dispatch(
        product: SeachemCalculations.Product,
        cur: BigDecimal,
        tgt: BigDecimal,
        vol: BigDecimal,
        scale: SeachemCalculations.UnitScale
    ): SeachemCalculations.CalculationResult = when (product) {
        SeachemCalculations.Product.FLOURISH ->
            SeachemCalculations.calculateFlourish(vol, "L")
        SeachemCalculations.Product.FLOURISH_TRACE ->
            SeachemCalculations.calculateFlourishTrace(vol, "L")
        SeachemCalculations.Product.ALKALINE_BUFFER ->
            SeachemCalculations.calculateAlkalineBuffer(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.ACID_BUFFER ->
            SeachemCalculations.calculateAcidBuffer(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.POTASSIUM_BICARBONATE ->
            SeachemCalculations.calculatePotassiumBicarbonate(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.NEUTRAL_REGULATOR ->
            SeachemCalculations.calculateNeutralRegulator(cur, tgt, vol, "L")
        SeachemCalculations.Product.EQUILIBRIUM ->
            SeachemCalculations.calculateEquilibrium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.FLOURISH_IRON ->
            SeachemCalculations.calculateFlourishIron(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.FLOURISH_NITROGEN ->
            SeachemCalculations.calculateFlourishNitrogen(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.FLOURISH_PHOSPHORUS ->
            SeachemCalculations.calculateFlourishPhosphorus(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.FLOURISH_POTASSIUM ->
            SeachemCalculations.calculateFlourishPotassium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_ADVANTAGE_CALCIUM ->
            SeachemCalculations.calculateReefAdvantageCalcium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_ADVANTAGE_MAGNESIUM ->
            SeachemCalculations.calculateReefAdvantageMagnesium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_ADVANTAGE_STRONTIUM ->
            SeachemCalculations.calculateReefAdvantageStrontium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_BUFFER ->
            SeachemCalculations.calculateReefBuffer(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_BUILDER ->
            SeachemCalculations.calculateReefBuilder(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_CALCIUM ->
            SeachemCalculations.calculateReefCalcium(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_CARBONATE ->
            SeachemCalculations.calculateReefCarbonate(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_COMPLETE ->
            SeachemCalculations.calculateReefComplete(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_FUSION_1 ->
            SeachemCalculations.calculateReefFusion1(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_FUSION_2 ->
            SeachemCalculations.calculateReefFusion2(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_IODIDE ->
            SeachemCalculations.calculateReefIodide(cur, tgt, vol, "L", scale)
        SeachemCalculations.Product.REEF_STRONTIUM ->
            SeachemCalculations.calculateReefStrontium(cur, tgt, vol, "L", scale)
    }

    override fun calculatePrimeDose(volumeLitres: Double): Double =
        Calculations.calculatePrimeDose(volumeLitres)

    override fun calculateStabilityDose(volumeLitres: Double): Double =
        Calculations.calculateStabilityDose(volumeLitres)

    override fun calculateSafe(volumeLitres: Double): Double =
        Calculations.calculateSafeGrams(volumeLitres)

    override fun calculateAptComplete(
        volumeLitres: Double,
        currentNitrate: Double
    ): CalculationsRepository.AptResult {
        val res = Calculations.calculateAptCompleteDose(volumeLitres, currentNitrate)
        return CalculationsRepository.AptResult(
            ml = res.ml,
            estimatedNitrateIncrease = res.estimatedNitrateIncrease,
            estimatedFinalNitrate = res.estimatedFinalNitrate
        )
    }
}
