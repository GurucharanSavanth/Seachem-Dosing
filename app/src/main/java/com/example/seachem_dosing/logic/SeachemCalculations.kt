package com.example.seachem_dosing.logic

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object SeachemCalculations {

    private val MC = MathContext(10, RoundingMode.HALF_UP)
    
    // Exact Conversion Factors
    private val MEQ_L_TO_DKH = BigDecimal("2.8")
    private val MEQ_L_TO_PPM = BigDecimal("50.0")
    private val DKH_TO_PPM = MEQ_L_TO_PPM.divide(MEQ_L_TO_DKH, MC)
    
    private val L_TO_US_GAL = BigDecimal("0.264172")

    enum class UnitScale(val displayName: String) {
        MEQ_L("meq/L"),
        DKH("dKH"),
        PPM("ppm"),
        PH("pH") // Added for pH based calcs safety
    }

    enum class Product {
        // Planted
        ALKALINE_BUFFER,
        ACID_BUFFER,
        EQUILIBRIUM,
        FLOURISH,
        FLOURISH_IRON,
        FLOURISH_NITROGEN,
        FLOURISH_PHOSPHORUS,
        FLOURISH_POTASSIUM,
        FLOURISH_TRACE,
        POTASSIUM_BICARBONATE,
        NEUTRAL_REGULATOR, // Added
        
        // Saltwater
        REEF_ADVANTAGE_CALCIUM,
        REEF_ADVANTAGE_MAGNESIUM,
        REEF_ADVANTAGE_STRONTIUM,
        REEF_BUFFER,
        REEF_BUILDER,
        REEF_CALCIUM,
        REEF_CARBONATE,
        REEF_COMPLETE,
        REEF_FUSION_1,
        REEF_FUSION_2,
        REEF_IODIDE,
        REEF_STRONTIUM
    }

    data class CalculationResult(
        val primaryValue: BigDecimal,
        val primaryUnit: String,
        val secondaryValue: BigDecimal,
        val secondaryUnit: String
    )

    // --- Helpers ---

    private fun toUsGal(volume: BigDecimal, unit: String): BigDecimal {
        return if (unit == "US") volume 
        else if (unit == "UK") volume.multiply(BigDecimal("1.20095"), MC)
        else volume.multiply(L_TO_US_GAL, MC)
    }
    
    private fun toLitres(volume: BigDecimal, unit: String): BigDecimal {
        return if (unit == "L") volume
        else if (unit == "US") volume.divide(L_TO_US_GAL, MC)
        else volume.multiply(BigDecimal("4.54609"), MC)
    }

    fun convertInput(value: BigDecimal, fromScale: UnitScale, toBaseUnit: UnitScale): BigDecimal {
        if (fromScale == toBaseUnit) return value
        if (fromScale == UnitScale.PH || toBaseUnit == UnitScale.PH) return value // No conversion for pH

        // Normalize to meq/L first
        val meqL = when (fromScale) {
            UnitScale.MEQ_L -> value
            UnitScale.DKH -> value.divide(MEQ_L_TO_DKH, MC)
            UnitScale.PPM -> value.divide(MEQ_L_TO_PPM, MC)
            else -> value
        }

        // Convert to target base unit
        return when (toBaseUnit) {
            UnitScale.MEQ_L -> meqL
            UnitScale.DKH -> meqL.multiply(MEQ_L_TO_DKH, MC)
            UnitScale.PPM -> meqL.multiply(MEQ_L_TO_PPM, MC)
            else -> meqL
        }
    }
    
    private fun format(val1: BigDecimal): BigDecimal {
        return val1.setScale(3, RoundingMode.HALF_UP)
    }

    // --- Formulas ---

    fun calculateAlkalineBuffer(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        // Base: meq/L
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)
        
        val diff = desMeq.subtract(curMeq, MC)
        val grams = diff.multiply(BigDecimal("3.5"), MC).multiply(volGal.divide(BigDecimal("10"), MC), MC)
        val tspns = grams.divide(BigDecimal("7"), MC)
        
        return CalculationResult(format(grams), "g", format(tspns), "tsp")
    }
    
    fun calculateNeutralRegulator(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String): CalculationResult {
        // Based on Seachem Neutral Regulator: 1 tsp (5g) for every 40-80 L once or twice a month.
        // Legacy app logic: baseGramsPerLitre based on KH?
        // Seachem instructions: "Use 5 g (1 level teaspoon) for every 40–80 L"
        // Let's use a simplified dosing for pH adjustment:
        // Assume 5g per 40L for significant adjustment.
        // Legacy formula was: `base * litres * (current - target) / 0.5`.
        // Let's use: (Current pH - Target pH) / 0.5 * (5g / 40L) * Volume(L).
        // If Target > Current, NR raises pH too, but let's stick to magnitude diff.
        
        val liters = toLitres(volume, volumeUnit)
        val diff = current.subtract(desired, MC).abs()
        
        // 5g per 40L per 0.5 pH shift (approx)
        val gramsPerLiterPer05 = BigDecimal("5").divide(BigDecimal("40"), MC)
        
        val steps = diff.divide(BigDecimal("0.5"), MC)
        val grams = steps.multiply(gramsPerLiterPer05, MC).multiply(liters, MC)
        val tspns = grams.divide(BigDecimal("5"), MC)
        
        return CalculationResult(format(grams), "g", format(tspns), "tsp")
    }
    
    fun calculateAcidBuffer(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val liters = toLitres(volume, volumeUnit)
        val curDkh = convertInput(current, inputScale, UnitScale.DKH)
        val desDkh = convertInput(desired, inputScale, UnitScale.DKH)
        val diff = curDkh.subtract(desDkh, MC)
        
        if (diff.toDouble() <= 0) return CalculationResult(BigDecimal.ZERO, "g", BigDecimal.ZERO, "tsp")
        
        val coeff = BigDecimal("0.01339")
        val grams = diff.multiply(coeff, MC).multiply(liters, MC)
        val tspns = grams.divide(BigDecimal("8"), MC)
        
        return CalculationResult(format(grams), "g", format(tspns), "tsp")
    }

    fun calculateEquilibrium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)

        val diff = desMeq.subtract(curMeq, MC)
        val grams = volGal.divide(BigDecimal("20"), MC).multiply(BigDecimal("16").multiply(diff, MC), MC)
        val tbsp = grams.divide(BigDecimal("16"), MC)

        return CalculationResult(format(grams), "g", format(tbsp), "tbsp")
    }
    
    fun calculatePotassiumBicarbonate(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val liters = toLitres(volume, volumeUnit)
        val curDkh = convertInput(current, inputScale, UnitScale.DKH)
        val desDkh = convertInput(desired, inputScale, UnitScale.DKH)
        
        val diff = desDkh.subtract(curDkh, MC)
        val purity = BigDecimal("1.0")
        
        val grams = diff.multiply(BigDecimal("0.0357"), MC).multiply(liters, MC).divide(purity, MC)
        val tsp = grams.divide(BigDecimal("5"), MC)
        
        return CalculationResult(format(grams), "g", format(tsp), "tsp")
    }

    fun calculateReefBuffer(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)
        
        val diff = desMeq.subtract(curMeq, MC)
        val tspns = diff.divide(BigDecimal("0.5"), MC).multiply(volGal.divide(BigDecimal("40"), MC), MC)
        val grams = tspns.multiply(BigDecimal("5"), MC)
        return CalculationResult(format(tspns), "tsp", format(grams), "g")
    }

    fun calculateReefBuilder(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)
        
        val diff = desMeq.subtract(curMeq, MC)
        val grams = BigDecimal("0.32").multiply(volGal.multiply(diff, MC), MC)
        val tspns = grams.divide(BigDecimal("6"), MC)
        return CalculationResult(format(grams), "g", format(tspns), "tsp")
    }

    fun calculateReefCarbonate(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)
        
        val diff = desMeq.subtract(curMeq, MC)
        val ml = diff.multiply(volGal, MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefFusion2(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curMeq = convertInput(current, inputScale, UnitScale.MEQ_L)
        val desMeq = convertInput(desired, inputScale, UnitScale.MEQ_L)
        
        val diff = desMeq.subtract(curMeq, MC)
        val ml = volGal.divide(BigDecimal("6.5"), MC).multiply(diff.divide(BigDecimal("0.176"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefAdvantageCalcium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val tspns = BigDecimal("0.0019").multiply(volGal, MC).multiply(diff, MC)
        val grams = tspns.multiply(BigDecimal("5"), MC)
        return CalculationResult(format(tspns), "tsp", format(grams), "g")
    }

    fun calculateReefAdvantageMagnesium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val tspns = BigDecimal("0.0095").multiply(volGal, MC).multiply(diff, MC)
        val grams = tspns.multiply(BigDecimal("5"), MC)
        return CalculationResult(format(tspns), "tsp", format(grams), "g")
    }

    fun calculateReefAdvantageStrontium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val grams = volGal.multiply(diff, MC).divide(BigDecimal("7.5"), MC)
        val tspns = grams.divide(BigDecimal("6"), MC)
        return CalculationResult(format(grams), "g", format(tspns), "tsp")
    }

    fun calculateReefCalcium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val term1 = diff.divide(BigDecimal("3"), MC)
        val term2 = volGal.divide(BigDecimal("20"), MC)
        val ml = term1.multiply(term2, MC).multiply(BigDecimal("5"), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefComplete(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = BigDecimal("0.025").multiply(volGal, MC).multiply(diff, MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefFusion1(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = volGal.divide(BigDecimal("6.5"), MC).multiply(diff.divide(BigDecimal("4"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefIodide(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = BigDecimal("0.5").multiply(volGal, MC).multiply(diff, MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateReefStrontium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = BigDecimal("0.4").multiply(volGal, MC).multiply(diff, MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    // Flourish (PPM Base for nutrients)
    
    fun calculateFlourish(volume: BigDecimal, volumeUnit: String): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val ml = volGal.divide(BigDecimal("60"), MC).multiply(BigDecimal("5"), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateFlourishIron(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = volGal.divide(BigDecimal("50"), MC).multiply(diff.multiply(BigDecimal("20"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }
    
    fun calculateFlourishNitrogen(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = diff.multiply(volGal.multiply(BigDecimal("0.25"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateFlourishPhosphorus(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = volGal.divide(BigDecimal("20"), MC).multiply(diff.multiply(BigDecimal("16.6"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateFlourishPotassium(current: BigDecimal, desired: BigDecimal, volume: BigDecimal, volumeUnit: String, inputScale: UnitScale): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val curPpm = convertInput(current, inputScale, UnitScale.PPM)
        val desPpm = convertInput(desired, inputScale, UnitScale.PPM)
        val diff = desPpm.subtract(curPpm, MC)
        
        val ml = volGal.divide(BigDecimal("30"), MC).multiply(diff.multiply(BigDecimal("2.5"), MC), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }

    fun calculateFlourishTrace(volume: BigDecimal, volumeUnit: String): CalculationResult {
        val volGal = toUsGal(volume, volumeUnit)
        val ml = volGal.divide(BigDecimal("20"), MC).multiply(BigDecimal("5"), MC)
        val caps = ml.divide(BigDecimal("5"), MC)
        return CalculationResult(format(ml), "mL", format(caps), "caps")
    }
    
    // Sand and Gravel
    fun calculateGravel(length: BigDecimal, width: BigDecimal, depth: BigDecimal, unit: String, divisor: BigDecimal, smallDivisor: BigDecimal): CalculationResult {
        var l = length
        var w = width
        var d = depth
        
        if (unit == "cm") {
            val inch = BigDecimal("2.54")
            l = l.divide(inch, MC)
            w = w.divide(inch, MC)
            d = d.divide(inch, MC)
        }
        
        val volume = l.multiply(w, MC).multiply(d, MC)
        
        if (divisor.compareTo(BigDecimal.ZERO) == 0) return CalculationResult(BigDecimal.ZERO, "bags", BigDecimal.ZERO, "bags")

        val bags = volume.divide(divisor, MC).setScale(0, RoundingMode.CEILING)
        val bagsSmall = if (smallDivisor.compareTo(BigDecimal.ZERO) > 0) 
            volume.divide(smallDivisor, MC).setScale(0, RoundingMode.CEILING) 
            else BigDecimal.ZERO
            
        return CalculationResult(bags, "bags (Standard)", bagsSmall, "bags (Small/7kg)")
    }
}