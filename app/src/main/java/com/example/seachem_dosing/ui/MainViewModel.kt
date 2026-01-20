package com.example.seachem_dosing.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.seachem_dosing.ai.AiInsightState
import com.example.seachem_dosing.ai.ChatMessage
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.logic.SeachemCalculations
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * Main ViewModel for the Seachem Dosing app.
 * Manages all water parameters, volume settings, and calculation results.
 * Uses SavedStateHandle to persist data across process death.
 */
class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    enum class AquariumProfile(val id: String) {
        FRESHWATER("freshwater"),
        SALTWATER("saltwater"),
        POND("pond"); // Strings kept for compatibility, UI renamed to "Sand and Gravel"

        companion object {
            fun fromId(id: String): AquariumProfile? {
                return entries.firstOrNull { it.id == id }
            }
        }
    }

    // Persist Profile ID, expose as Enum
    private val _profileId: MutableLiveData<String> = savedStateHandle.getLiveData("profile_id", AquariumProfile.FRESHWATER.id)
    val profile: LiveData<AquariumProfile> = _profileId.map { id ->
        AquariumProfile.fromId(id) ?: AquariumProfile.FRESHWATER
    }

    private val _aiInsight = MutableLiveData(AiInsightState())
    val aiInsight: LiveData<AiInsightState> = _aiInsight

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    // ==================== Volume Settings ====================

    val volume: MutableLiveData<Double> = savedStateHandle.getLiveData("volume", 10.0)
    val volumeUnit: MutableLiveData<String> = savedStateHandle.getLiveData("volume_unit", "US") // L, US, UK
    val volumeMode: MutableLiveData<String> = savedStateHandle.getLiveData("volume_mode", "direct") // direct or lbh
    val dimLength: MutableLiveData<Double> = savedStateHandle.getLiveData("dim_length", 60.0)
    val dimBreadth: MutableLiveData<Double> = savedStateHandle.getLiveData("dim_breadth", 30.0)
    val dimHeight: MutableLiveData<Double> = savedStateHandle.getLiveData("dim_height", 40.0)
    val dimUnit: MutableLiveData<String> = savedStateHandle.getLiveData("dim_unit", "cm") // cm, in, ft

    // ==================== Water Parameters (Restored) ====================

    val ammonia: MutableLiveData<Double> = savedStateHandle.getLiveData("ammonia", 0.0)
    val nitrite: MutableLiveData<Double> = savedStateHandle.getLiveData("nitrite", 0.0)
    val nitrate: MutableLiveData<Double> = savedStateHandle.getLiveData("nitrate", 15.0)
    val gh: MutableLiveData<Double> = savedStateHandle.getLiveData("gh", 4.0)
    val kh: MutableLiveData<Double> = savedStateHandle.getLiveData("kh", 6.0)
    val ph: MutableLiveData<Double> = savedStateHandle.getLiveData("ph", 7.2)
    val temperature: MutableLiveData<Double> = savedStateHandle.getLiveData("temperature", 26.0)
    val salinity: MutableLiveData<Double> = savedStateHandle.getLiveData("salinity", 35.0)
    val alkalinity: MutableLiveData<Double> = savedStateHandle.getLiveData("alkalinity", 8.0)
    val calcium: MutableLiveData<Double> = savedStateHandle.getLiveData("calcium", 420.0)
    val magnesium: MutableLiveData<Double> = savedStateHandle.getLiveData("magnesium", 1300.0)
    val phosphate: MutableLiveData<Double> = savedStateHandle.getLiveData("phosphate", 0.05)
    val dissolvedOxygen: MutableLiveData<Double> = savedStateHandle.getLiveData("dissolved_oxygen", 7.5)
    
    // New Parameters
    val potassium: MutableLiveData<Double> = savedStateHandle.getLiveData("potassium", 15.0) // mg/L
    val iron: MutableLiveData<Double> = savedStateHandle.getLiveData("iron", 0.1) // mg/L
    val strontium: MutableLiveData<Double> = savedStateHandle.getLiveData("strontium", 8.0) // mg/L (Saltwater)
    val iodide: MutableLiveData<Double> = savedStateHandle.getLiveData("iodide", 0.06) // mg/L (Saltwater)

    val ghUnit: MutableLiveData<String> = savedStateHandle.getLiveData("gh_unit", "dh") // dh or ppm
    val khUnit: MutableLiveData<String> = savedStateHandle.getLiveData("kh_unit", "dh") // dh or ppm

    // ==================== Calculator Inputs (Universal) ====================
    // We use a separate map for calculator inputs, but backing them with SavedStateHandle
    // requires dynamic keys. For simplicity and since these are "scratchpad" values,
    // we'll keep them in memory for now, but ensure key parameters sync TO them.
    // Using ConcurrentHashMap for thread-safety with coroutines.

    private val _calcInputs = ConcurrentHashMap<String, MutableLiveData<Double>>()
    
    fun getInput(id: String): LiveData<Double> {
        return _calcInputs.getOrPut(id) { MutableLiveData(0.0) }
    }
    
    fun setInput(id: String, value: Double) {
        _calcInputs.getOrPut(id) { MutableLiveData(0.0) }.value = value
    }

    // Substrate Inputs
    val subLength: MutableLiveData<Double> = savedStateHandle.getLiveData("sub_length", 0.0)
    val subWidth: MutableLiveData<Double> = savedStateHandle.getLiveData("sub_width", 0.0)
    val subDepth: MutableLiveData<Double> = savedStateHandle.getLiveData("sub_depth", 0.0)
    val subProduct: MutableLiveData<Int> = savedStateHandle.getLiveData("sub_product", 0) // Index
    val subUnit: MutableLiveData<String> = savedStateHandle.getLiveData("sub_unit", "cm")

    // ==================== App Settings ====================
    val defaultWaterChangePercent: MutableLiveData<Double> = savedStateHandle.getLiveData("default_wc_percent", 20.0)

    fun setDefaultWaterChangePercent(value: Double) {
        defaultWaterChangePercent.value = value
    }

    fun generateExportData(): String {
        // Safe JSON string escaping
        fun escapeJson(s: String?): String = s?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"profile\": \"${escapeJson(_profileId.value)}\",\n")
        sb.append("  \"volume\": ${volume.value ?: 0.0},\n")
        sb.append("  \"volumeUnit\": \"${escapeJson(volumeUnit.value)}\",\n")
        sb.append("  \"parameters\": {\n")
        sb.append("    \"ammonia\": ${ammonia.value ?: 0.0},\n")
        sb.append("    \"nitrite\": ${nitrite.value ?: 0.0},\n")
        sb.append("    \"nitrate\": ${nitrate.value ?: 0.0},\n")
        sb.append("    \"ph\": ${ph.value ?: 0.0},\n")
        sb.append("    \"gh\": ${gh.value ?: 0.0},\n")
        sb.append("    \"kh\": ${kh.value ?: 0.0},\n")
        sb.append("    \"temperature\": ${temperature.value ?: 0.0}\n")
        sb.append("  }\n")
        sb.append("}")
        return sb.toString()
    }
    
    // Legacy calculator state holders (required by older fragments if any reference remains)
    private val _khCurrent = MutableLiveData(0.0)
    private val _khTarget = MutableLiveData(4.0)
    private val _khPurity = MutableLiveData(0.99)
    private val _ghCurrent = MutableLiveData(0.0)
    private val _ghTarget = MutableLiveData(6.0)
    private val _phCurrent = MutableLiveData(7.5)
    private val _phTarget = MutableLiveData(7.0)
    private val _nrKh = MutableLiveData(4.0)
    private val _acidCurrentKh = MutableLiveData(6.0)
    private val _acidTargetKh = MutableLiveData(4.0)
    private val _goldPhCurrent = MutableLiveData(7.0)
    private val _goldPhTarget = MutableLiveData(7.5)

    // ==================== Calculation Methods ====================

    fun getEffectiveVolumeLitres(): Double {
        return if (volumeMode.value == "lbh") {
            Calculations.dimensionsToLitres(
                dimLength.value ?: 0.0,
                dimBreadth.value ?: 0.0,
                dimHeight.value ?: 0.0,
                dimUnit.value ?: "cm"
            )
        } else {
            Calculations.toLitres(volume.value ?: 0.0, volumeUnit.value ?: "L")
        }
    }
    
    private fun getEngineVolume(): BigDecimal = BigDecimal.valueOf(getEffectiveVolumeLitres())
    private fun getEngineVolumeUnit(): String = "L"

    // Generic Dosing Calculation
    fun calculateUniversal(
        productId: SeachemCalculations.Product,
        current: Double,
        target: Double,
        scale: SeachemCalculations.UnitScale
    ): SeachemCalculations.CalculationResult {
        return when (productId) {
            SeachemCalculations.Product.FLOURISH -> SeachemCalculations.calculateFlourish(getEngineVolume(), getEngineVolumeUnit())
            SeachemCalculations.Product.FLOURISH_TRACE -> SeachemCalculations.calculateFlourishTrace(getEngineVolume(), getEngineVolumeUnit())
            
            SeachemCalculations.Product.ALKALINE_BUFFER -> SeachemCalculations.calculateAlkalineBuffer(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.ACID_BUFFER -> SeachemCalculations.calculateAcidBuffer(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.POTASSIUM_BICARBONATE -> SeachemCalculations.calculatePotassiumBicarbonate(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.NEUTRAL_REGULATOR -> SeachemCalculations.calculateNeutralRegulator(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit()
            )
            SeachemCalculations.Product.EQUILIBRIUM -> SeachemCalculations.calculateEquilibrium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.FLOURISH_IRON -> SeachemCalculations.calculateFlourishIron(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.FLOURISH_NITROGEN -> SeachemCalculations.calculateFlourishNitrogen(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.FLOURISH_PHOSPHORUS -> SeachemCalculations.calculateFlourishPhosphorus(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.FLOURISH_POTASSIUM -> SeachemCalculations.calculateFlourishPotassium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            
            SeachemCalculations.Product.REEF_ADVANTAGE_CALCIUM -> SeachemCalculations.calculateReefAdvantageCalcium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_ADVANTAGE_MAGNESIUM -> SeachemCalculations.calculateReefAdvantageMagnesium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_ADVANTAGE_STRONTIUM -> SeachemCalculations.calculateReefAdvantageStrontium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_BUFFER -> SeachemCalculations.calculateReefBuffer(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_BUILDER -> SeachemCalculations.calculateReefBuilder(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_CALCIUM -> SeachemCalculations.calculateReefCalcium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_CARBONATE -> SeachemCalculations.calculateReefCarbonate(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_COMPLETE -> SeachemCalculations.calculateReefComplete(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_FUSION_1 -> SeachemCalculations.calculateReefFusion1(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_FUSION_2 -> SeachemCalculations.calculateReefFusion2(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_IODIDE -> SeachemCalculations.calculateReefIodide(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
            SeachemCalculations.Product.REEF_STRONTIUM -> SeachemCalculations.calculateReefStrontium(
                BigDecimal.valueOf(current), BigDecimal.valueOf(target), getEngineVolume(), getEngineVolumeUnit(), scale
            )
        }
    }
    
    fun calculateSubstrate(productIndex: Int): SeachemCalculations.CalculationResult {
        val specs = listOf(
            Pair(8250, 9000), // Flourite
            Pair(7250, 0),    // Flourite Black
            Pair(8000, 0),    // Flourite Black Sand
            Pair(8250, 0),    // Flourite Dark
            Pair(8250, 0),    // Flourite Red
            Pair(8750, 0),    // Flourite Sand
            Pair(8500, 0),    // Gray Coast
            Pair(10500, 0),   // Meridian
            Pair(8000, 0),    // Onyx
            Pair(8250, 0),    // Onyx Sand
            Pair(9750, 0)     // Pearl Beach
        )
        
        val safeIndex = productIndex.coerceIn(0, specs.lastIndex)
        val (div, divSmall) = specs[safeIndex]
        
        return SeachemCalculations.calculateGravel(
            BigDecimal.valueOf(subLength.value ?: 0.0),
            BigDecimal.valueOf(subWidth.value ?: 0.0),
            BigDecimal.valueOf(subDepth.value ?: 0.0),
            subUnit.value ?: "cm",
            BigDecimal(div),
            BigDecimal(divSmall)
        )
    }

    // ==================== Legacy/Utility Wrappers ====================

    fun calculatePrimeDose(): Double = Calculations.calculatePrimeDose(getEffectiveVolumeLitres())
    fun calculateStabilityDose(): Double = Calculations.calculateStabilityDose(getEffectiveVolumeLitres())
    fun calculateSafe(): Calculations.GoldBufferResult {
        val litres = getEffectiveVolumeLitres()
        val grams = Calculations.calculateSafeGrams(litres)
        return Calculations.GoldBufferResult(grams, grams > 0)
    }
    fun calculateSafeSimple(): Double = Calculations.calculateSafeGrams(getEffectiveVolumeLitres())
    fun calculateAptComplete(): Calculations.AptResult {
        val litres = getEffectiveVolumeLitres()
        return Calculations.calculateAptCompleteDose(litres, nitrate.value ?: 0.0)
    }
    
    fun calculateWaterChangeLitres(percent: Double): Double {
        return getEffectiveVolumeLitres() * (percent / 100.0)
    }
    
    // Status helpers (for dashboard)
    data class ParameterStatus(val status: Status, val displayValue: String)
    enum class Status { GOOD, WARNING, DANGER, INFO }

    fun getAmmoniaStatus(): ParameterStatus {
        val value = ammonia.value ?: 0.0
        return ParameterStatus(if (value > 0) Status.DANGER else Status.GOOD, "${value} ppm")
    }
    fun getNitriteStatus(): ParameterStatus {
        val value = nitrite.value ?: 0.0
        return ParameterStatus(if (value > 0) Status.DANGER else Status.GOOD, "${value} ppm")
    }
    fun getNitrateStatus(): ParameterStatus {
        val value = nitrate.value ?: 0.0
        return ParameterStatus(if (value > 50) Status.WARNING else Status.GOOD, "${value.toInt()} ppm")
    }

    // ==================== Missing Helper Methods (Restored) ====================
    
    fun getGhInDegrees(): Double {
        val value = gh.value ?: 0.0
        return if (ghUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }

    fun getKhInDegrees(): Double {
        val value = kh.value ?: 0.0
        return if (khUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }
    
    fun updateHardnessUnit(unit: String) {
        val targetUnit = if (unit == "ppm") "ppm" else "dh"
        val currentGhUnit = ghUnit.value ?: "dh"
        val currentKhUnit = khUnit.value ?: "dh"
        if (currentGhUnit != targetUnit) {
            gh.value = convertHardness(gh.value ?: 0.0, currentGhUnit, targetUnit)
            ghUnit.value = targetUnit
        }
        if (currentKhUnit != targetUnit) {
            kh.value = convertHardness(kh.value ?: 0.0, currentKhUnit, targetUnit)
            khUnit.value = targetUnit
        }
        syncGhFromParams()
        syncKhFromParams()
    }
    
    private fun convertHardness(value: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> value
            fromUnit == "ppm" && toUnit == "dh" -> Calculations.ppmToDh(value)
            fromUnit == "dh" && toUnit == "ppm" -> Calculations.dhToPpm(value)
            else -> value
        }
    }

    fun syncKhFromParams() {
        val khVal = getKhInDegrees()
        setInput("alkaline_buffer_current", khVal)
        setInput("acid_buffer_current", khVal)
        setInput("reef_builder_current", khVal)
        setInput("reef_buffer_current", khVal)
        setInput("reef_carbonate_current", khVal)
        setInput("khco3_current", khVal) // For Potassium Bicarbonate
        // Legacy
        _khCurrent.value = khVal
        _nrKh.value = khVal
        _acidCurrentKh.value = khVal
    }

    fun syncGhFromParams() {
        val ghVal = getGhInDegrees()
        setInput("equilibrium_current", ghVal)
        _ghCurrent.value = ghVal
    }

    fun syncPhFromParams() {
        val currentPh = ph.value ?: 7.0
        setInput("neutral_regulator_current", currentPh)
        _phCurrent.value = currentPh
        _goldPhCurrent.value = currentPh
    }
    
    fun syncNewParams() {
        setInput("flourish_potassium_current", potassium.value ?: 0.0)
        setInput("flourish_iron_current", iron.value ?: 0.0)
        // Nitrogen often uses Nitrate as proxy if not specific N test
        // setInput("flourish_nitrogen_current", ...) 
        // Phosphate
        setInput("flourish_phosphorus_current", phosphate.value ?: 0.0)
        
        // Saltwater
        setInput("reef_adv_calcium_current", calcium.value ?: 0.0)
        setInput("reef_calcium_current", calcium.value ?: 0.0)
        setInput("reef_complete_current", calcium.value ?: 0.0)
        setInput("reef_fusion1_current", calcium.value ?: 0.0)
        
        setInput("reef_adv_magnesium_current", magnesium.value ?: 0.0)
        
        setInput("reef_adv_strontium_current", strontium.value ?: 0.0)
        setInput("reef_strontium_current", strontium.value ?: 0.0)
        
        setInput("reef_iodide_current", iodide.value ?: 0.0)
        
        // Alkalinity handled by syncKhFromParams
    }
    
    fun resetAll() {
        // Reset directly to SavedStateHandle backed values
        volume.value = 10.0
        volumeUnit.value = "US"
        volumeMode.value = "direct"
        dimLength.value = 60.0
        dimBreadth.value = 30.0
        dimHeight.value = 40.0
        dimUnit.value = "cm"
        ammonia.value = 0.0
        nitrite.value = 0.0
        nitrate.value = 15.0
        gh.value = 4.0
        kh.value = 6.0
        ph.value = 7.2
        temperature.value = 26.0
        salinity.value = 35.0
        alkalinity.value = 8.0
        calcium.value = 420.0
        magnesium.value = 1300.0
        phosphate.value = 0.05
        dissolvedOxygen.value = 7.5
        potassium.value = 15.0
        iron.value = 0.1
        strontium.value = 8.0
        iodide.value = 0.06
        ghUnit.value = "dh"
        khUnit.value = "dh"
        defaultWaterChangePercent.value = 20.0
        _aiInsight.value = AiInsightState()
        _chatMessages.value = emptyList()
        // Reset calc inputs
        _calcInputs.clear()
        
        applyProfileDefaults(profile.value ?: AquariumProfile.FRESHWATER)
    }

    // ==================== Input Validation ====================

    private fun coerceNonNegative(value: Double): Double = if (value < 0) 0.0 else value

    private fun coercePh(value: Double): Double = value.coerceIn(0.0, 14.0)

    private fun coerceTemperature(value: Double): Double = value.coerceIn(-5.0, 50.0)

    private fun coercePercentage(value: Double): Double = value.coerceIn(0.0, 100.0)

    private fun coerceSalinity(value: Double): Double = value.coerceIn(0.0, 50.0)

    // ==================== Setters ====================

    fun setVolume(value: Double) { volume.value = coerceNonNegative(value) }
    fun setVolumeUnit(unit: String) { volumeUnit.value = unit }
    fun setVolumeMode(mode: String) { volumeMode.value = mode }
    fun setDimLength(value: Double) { dimLength.value = coerceNonNegative(value) }
    fun setDimBreadth(value: Double) { dimBreadth.value = coerceNonNegative(value) }
    fun setDimHeight(value: Double) { dimHeight.value = coerceNonNegative(value) }
    fun setDimUnit(unit: String) { dimUnit.value = unit }
    
    fun setSubLength(value: Double) { subLength.value = value }
    fun setSubWidth(value: Double) { subWidth.value = value }
    fun setSubDepth(value: Double) { subDepth.value = value }
    fun setSubProduct(index: Int) { subProduct.value = index }
    fun setSubUnit(unit: String) { subUnit.value = unit }

    // Salt Mix Inputs
    // Persist salt mix inputs if desired, or keep ephemeral. 
    // User requested "any value input in calculator under the drop down...". 
    // Best to persist.
    val saltMixProduct: MutableLiveData<Int> = savedStateHandle.getLiveData("salt_mix_product", 0)
    val saltMixVolume: MutableLiveData<Double> = savedStateHandle.getLiveData("salt_mix_volume", 5.0)
    val saltMixCurrentPpt: MutableLiveData<Double> = savedStateHandle.getLiveData("salt_mix_current_ppt", 0.0)
    val saltMixDesiredPpt: MutableLiveData<Double> = savedStateHandle.getLiveData("salt_mix_desired_ppt", 35.0)

    fun setSaltMixProduct(index: Int) { saltMixProduct.value = index }
    fun setSaltMixVolume(value: Double) { saltMixVolume.value = value }
    fun setSaltMixCurrentPpt(value: Double) { saltMixCurrentPpt.value = value }
    fun setSaltMixDesiredPpt(value: Double) { saltMixDesiredPpt.value = value }
    
    fun calculateSaltMix(): com.example.seachem_dosing.logic.SaltMixCalculations.SaltMixResult? {
        val products = com.example.seachem_dosing.logic.SaltMixCalculations.SALT_MIX_PRODUCTS.keys.toList()
        val index = saltMixProduct.value ?: 0
        if (index !in products.indices) return null
        
        return com.example.seachem_dosing.logic.SaltMixCalculations.calculateSaltMix(
            products[index],
            saltMixVolume.value ?: 0.0,
            saltMixCurrentPpt.value ?: 0.0,
            saltMixDesiredPpt.value ?: 0.0
        )
    }

    fun setProfile(profile: AquariumProfile) {
        _profileId.value = profile.id
        applyProfileDefaults(profile)
    }
    
    private fun applyProfileDefaults(profile: AquariumProfile) {
        when (profile) {
            AquariumProfile.FRESHWATER -> {
                ammonia.value = 0.0
                nitrite.value = 0.0
                nitrate.value = 15.0
                gh.value = 4.0
                kh.value = 6.0
                ph.value = 7.2
                temperature.value = 26.0
                ghUnit.value = "dh"
                khUnit.value = "dh"
                syncGhFromParams()
                syncKhFromParams()
                syncPhFromParams()
                syncNewParams()
            }
            AquariumProfile.SALTWATER -> {
                ammonia.value = 0.0
                nitrite.value = 0.0
                gh.value = 0.0
                kh.value = 0.0
                dissolvedOxygen.value = 0.0
                salinity.value = 35.0
                alkalinity.value = 8.0
                calcium.value = 420.0
                magnesium.value = 1300.0
                nitrate.value = 10.0
                phosphate.value = 0.05
                ph.value = 8.2
                temperature.value = 26.0
                syncPhFromParams()
                syncNewParams()
            }
            AquariumProfile.POND -> {
                ammonia.value = 0.0
                nitrite.value = 0.0
                nitrate.value = 20.0
                gh.value = 0.0
                kh.value = 5.0
                ph.value = 7.4
                temperature.value = 22.0
                dissolvedOxygen.value = 7.5
                khUnit.value = "dh"
                syncKhFromParams()
                syncPhFromParams()
            }
        }
    }
    
    // Parameter setters with validation
    fun setAmmonia(value: Double) { ammonia.value = coerceNonNegative(value) }
    fun setNitrite(value: Double) { nitrite.value = coerceNonNegative(value) }
    fun setNitrate(value: Double) { nitrate.value = coerceNonNegative(value) }
    fun setGh(value: Double) { gh.value = coerceNonNegative(value) }
    fun setKh(value: Double) { kh.value = coerceNonNegative(value) }
    fun setPh(value: Double) { ph.value = coercePh(value) }
    fun setTemperature(value: Double) { temperature.value = coerceTemperature(value) }
    fun setSalinity(value: Double) { salinity.value = coerceSalinity(value) }
    fun setAlkalinity(value: Double) { alkalinity.value = coerceNonNegative(value) }
    fun setCalcium(value: Double) { calcium.value = coerceNonNegative(value) }
    fun setMagnesium(value: Double) { magnesium.value = coerceNonNegative(value) }
    fun setPhosphate(value: Double) { phosphate.value = coerceNonNegative(value) }
    fun setDissolvedOxygen(value: Double) { dissolvedOxygen.value = coerceNonNegative(value) }
    fun setPotassium(value: Double) { potassium.value = coerceNonNegative(value) }
    fun setIron(value: Double) { iron.value = coerceNonNegative(value) }
    fun setStrontium(value: Double) { strontium.value = coerceNonNegative(value) }
    fun setIodide(value: Double) { iodide.value = coerceNonNegative(value) }
    fun setGhUnit(unit: String) { ghUnit.value = unit }
    fun setKhUnit(unit: String) { khUnit.value = unit }
    
    // Legacy setters (kept for compatibility if referenced)
    fun setKhCurrent(value: Double) { _khCurrent.value = value }
    fun setKhTarget(value: Double) { _khTarget.value = value }
    fun setKhPurity(value: Double) { _khPurity.value = value }
    fun setGhCurrent(value: Double) { _ghCurrent.value = value }
    fun setGhTarget(value: Double) { _ghTarget.value = value }
    fun setPhCurrent(value: Double) { _phCurrent.value = value }
    fun setPhTarget(value: Double) { _phTarget.value = value }
    fun setNrKh(value: Double) { _nrKh.value = value }
    fun setAcidCurrentKh(value: Double) { _acidCurrentKh.value = value }
    fun setAcidTargetKh(value: Double) { _acidTargetKh.value = value }
    fun setGoldPhCurrent(value: Double) { _goldPhCurrent.value = value }
    fun setGoldPhTarget(value: Double) { _goldPhTarget.value = value }
}