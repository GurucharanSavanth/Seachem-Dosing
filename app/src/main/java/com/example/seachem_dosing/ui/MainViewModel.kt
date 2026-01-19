package com.example.seachem_dosing.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.seachem_dosing.ai.AiInsightState
import com.example.seachem_dosing.ai.ChatMessage
import com.example.seachem_dosing.ai.ChatRole
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.logic.SeachemCalculations
import java.math.BigDecimal

/**
 * Main ViewModel for the Seachem Dosing app.
 * Manages all water parameters, volume settings, and calculation results.
 */
class MainViewModel : ViewModel() {

    enum class AquariumProfile(val id: String) {
        FRESHWATER("freshwater"),
        SALTWATER("saltwater"),
        POND("pond"); // Strings kept for compatibility, UI renamed to "Sand and Gravel"
        
        companion object {
            fun fromId(id: String): AquariumProfile? {
                return values().firstOrNull { it.id == id }
            }
        }
    }

    private val _profile = MutableLiveData(AquariumProfile.FRESHWATER)
    val profile: LiveData<AquariumProfile> = _profile

    private val _aiInsight = MutableLiveData(AiInsightState())
    val aiInsight: LiveData<AiInsightState> = _aiInsight

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    // ==================== Volume Settings ====================

    private val _volume = MutableLiveData(10.0)
    val volume: LiveData<Double> = _volume

    private val _volumeUnit = MutableLiveData("US") // L, US, UK
    val volumeUnit: LiveData<String> = _volumeUnit

    private val _volumeMode = MutableLiveData("direct") // direct or lbh
    val volumeMode: LiveData<String> = _volumeMode

    private val _dimLength = MutableLiveData(60.0)
    val dimLength: LiveData<Double> = _dimLength

    private val _dimBreadth = MutableLiveData(30.0)
    val dimBreadth: LiveData<Double> = _dimBreadth

    private val _dimHeight = MutableLiveData(40.0)
    val dimHeight: LiveData<Double> = _dimHeight

    private val _dimUnit = MutableLiveData("cm") // cm, in, ft
    val dimUnit: LiveData<String> = _dimUnit

    // ==================== Water Parameters (Restored) ====================

    private val _ammonia = MutableLiveData(0.0)
    val ammonia: LiveData<Double> = _ammonia

    private val _nitrite = MutableLiveData(0.0)
    val nitrite: LiveData<Double> = _nitrite

    private val _nitrate = MutableLiveData(15.0)
    val nitrate: LiveData<Double> = _nitrate

    private val _gh = MutableLiveData(4.0)
    val gh: LiveData<Double> = _gh

    private val _kh = MutableLiveData(6.0)
    val kh: LiveData<Double> = _kh

    private val _ph = MutableLiveData(7.2)
    val ph: LiveData<Double> = _ph

    private val _temperature = MutableLiveData(26.0)
    val temperature: LiveData<Double> = _temperature

    private val _salinity = MutableLiveData(35.0)
    val salinity: LiveData<Double> = _salinity

    private val _alkalinity = MutableLiveData(8.0)
    val alkalinity: LiveData<Double> = _alkalinity

    private val _calcium = MutableLiveData(420.0)
    val calcium: LiveData<Double> = _calcium

    private val _magnesium = MutableLiveData(1300.0)
    val magnesium: LiveData<Double> = _magnesium

    private val _phosphate = MutableLiveData(0.05)
    val phosphate: LiveData<Double> = _phosphate

    private val _dissolvedOxygen = MutableLiveData(7.5)
    val dissolvedOxygen: LiveData<Double> = _dissolvedOxygen
    
    // New Parameters
    private val _potassium = MutableLiveData(15.0) // mg/L
    val potassium: LiveData<Double> = _potassium
    
    private val _iron = MutableLiveData(0.1) // mg/L
    val iron: LiveData<Double> = _iron
    
    private val _strontium = MutableLiveData(8.0) // mg/L (Saltwater)
    val strontium: LiveData<Double> = _strontium
    
    private val _iodide = MutableLiveData(0.06) // mg/L (Saltwater)
    val iodide: LiveData<Double> = _iodide

    private val _ghUnit = MutableLiveData("dh") // dh or ppm
    val ghUnit: LiveData<String> = _ghUnit

    private val _khUnit = MutableLiveData("dh") // dh or ppm
    val khUnit: LiveData<String> = _khUnit

    // ==================== Calculator Inputs (Universal) ====================
    
    private val _calcInputs = mutableMapOf<String, MutableLiveData<Double>>()
    
    fun getInput(id: String): LiveData<Double> {
        return _calcInputs.getOrPut(id) { MutableLiveData(0.0) }
    }
    
    fun setInput(id: String, value: Double) {
        _calcInputs.getOrPut(id) { MutableLiveData(0.0) }.value = value
    }

    // Substrate Inputs
    private val _subLength = MutableLiveData(0.0)
    val subLength: LiveData<Double> = _subLength
    private val _subWidth = MutableLiveData(0.0)
    val subWidth: LiveData<Double> = _subWidth
    private val _subDepth = MutableLiveData(0.0)
    val subDepth: LiveData<Double> = _subDepth
    private val _subProduct = MutableLiveData(0) // Index
    val subProduct: LiveData<Int> = _subProduct
    private val _subUnit = MutableLiveData("cm")
    val subUnit: LiveData<String> = _subUnit

    // ==================== App Settings ====================
    private val _defaultWaterChangePercent = MutableLiveData(20.0)
    val defaultWaterChangePercent: LiveData<Double> = _defaultWaterChangePercent

    fun setDefaultWaterChangePercent(value: Double) {
        _defaultWaterChangePercent.value = value
    }

    fun generateExportData(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"profile\": \"${_profile.value?.id}\",\n")
        sb.append("  \"volume\": ${_volume.value},\n")
        sb.append("  \"volumeUnit\": \"${_volumeUnit.value}\",\n")
        sb.append("  \"parameters\": {\n")
        sb.append("    \"ammonia\": ${_ammonia.value},\n")
        sb.append("    \"nitrite\": ${_nitrite.value},\n")
        sb.append("    \"nitrate\": ${_nitrate.value},\n")
        sb.append("    \"ph\": ${_ph.value},\n")
        sb.append("    \"gh\": ${_gh.value},\n")
        sb.append("    \"kh\": ${_kh.value},\n")
        sb.append("    \"temperature\": ${_temperature.value}\n")
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
        return if (_volumeMode.value == "lbh") {
            Calculations.dimensionsToLitres(
                _dimLength.value ?: 0.0,
                _dimBreadth.value ?: 0.0,
                _dimHeight.value ?: 0.0,
                _dimUnit.value ?: "cm"
            )
        } else {
            Calculations.toLitres(_volume.value ?: 0.0, _volumeUnit.value ?: "L")
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
            BigDecimal.valueOf(_subLength.value ?: 0.0),
            BigDecimal.valueOf(_subWidth.value ?: 0.0),
            BigDecimal.valueOf(_subDepth.value ?: 0.0),
            _subUnit.value ?: "cm",
            BigDecimal(div),
            BigDecimal(divSmall)
        )
    }

    // ==================== Legacy/Utility Wrappers ====================

    fun calculatePrimeDose(): Double = Calculations.calculatePrimeDose(getEffectiveVolumeLitres())
    fun calculateStabilityDose(): Double = Calculations.calculateStabilityDose(getEffectiveVolumeLitres())
    fun calculateSafe(): Calculations.GoldBufferResult { 
        return Calculations.GoldBufferResult(0.0, false)
    }
    fun calculateSafeSimple(): Double = Calculations.calculateSafeGrams(getEffectiveVolumeLitres())
    fun calculateAptComplete(): Calculations.AptResult {
        val litres = getEffectiveVolumeLitres()
        return Calculations.calculateAptCompleteDose(litres, _nitrate.value ?: 0.0)
    }
    
    fun calculateWaterChangeLitres(percent: Double): Double {
        return getEffectiveVolumeLitres() * (percent / 100.0)
    }
    
    // Status helpers (for dashboard)
    data class ParameterStatus(val status: Status, val displayValue: String)
    enum class Status { GOOD, WARNING, DANGER, INFO }

    fun getAmmoniaStatus(): ParameterStatus {
        val value = _ammonia.value ?: 0.0
        return ParameterStatus(if (value > 0) Status.DANGER else Status.GOOD, "${value} ppm")
    }
    fun getNitriteStatus(): ParameterStatus {
        val value = _nitrite.value ?: 0.0
        return ParameterStatus(if (value > 0) Status.DANGER else Status.GOOD, "${value} ppm")
    }
    fun getNitrateStatus(): ParameterStatus {
        val value = _nitrate.value ?: 0.0
        return ParameterStatus(if (value > 50) Status.WARNING else Status.GOOD, "${value.toInt()} ppm")
    }

    // ==================== Missing Helper Methods (Restored) ====================
    
    fun getGhInDegrees(): Double {
        val value = _gh.value ?: 0.0
        return if (_ghUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }

    fun getKhInDegrees(): Double {
        val value = _kh.value ?: 0.0
        return if (_khUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }
    
    fun updateHardnessUnit(unit: String) {
        val targetUnit = if (unit == "ppm") "ppm" else "dh"
        val currentGhUnit = _ghUnit.value ?: "dh"
        val currentKhUnit = _khUnit.value ?: "dh"
        if (currentGhUnit != targetUnit) {
            _gh.value = convertHardness(_gh.value ?: 0.0, currentGhUnit, targetUnit)
            _ghUnit.value = targetUnit
        }
        if (currentKhUnit != targetUnit) {
            _kh.value = convertHardness(_kh.value ?: 0.0, currentKhUnit, targetUnit)
            _khUnit.value = targetUnit
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
        val kh = getKhInDegrees()
        setInput("alkaline_buffer_current", kh)
        setInput("acid_buffer_current", kh)
        setInput("reef_builder_current", kh)
        setInput("reef_buffer_current", kh)
        setInput("reef_carbonate_current", kh)
        setInput("khco3_current", kh) // For Potassium Bicarbonate
        // Legacy
        _khCurrent.value = kh
        _nrKh.value = kh
        _acidCurrentKh.value = kh
    }

    fun syncGhFromParams() {
        val gh = getGhInDegrees()
        setInput("equilibrium_current", gh)
        _ghCurrent.value = gh
    }

    fun syncPhFromParams() {
        val currentPh = _ph.value ?: 7.0
        setInput("neutral_regulator_current", currentPh)
        _phCurrent.value = currentPh
        _goldPhCurrent.value = currentPh
    }
    
    fun syncNewParams() {
        setInput("flourish_potassium_current", _potassium.value ?: 0.0)
        setInput("flourish_iron_current", _iron.value ?: 0.0)
        // Nitrogen often uses Nitrate as proxy if not specific N test
        // setInput("flourish_nitrogen_current", ...) 
        // Phosphate
        setInput("flourish_phosphorus_current", _phosphate.value ?: 0.0)
        
        // Saltwater
        setInput("reef_adv_calcium_current", _calcium.value ?: 0.0)
        setInput("reef_calcium_current", _calcium.value ?: 0.0)
        setInput("reef_complete_current", _calcium.value ?: 0.0)
        setInput("reef_fusion1_current", _calcium.value ?: 0.0)
        
        setInput("reef_adv_magnesium_current", _magnesium.value ?: 0.0)
        
        setInput("reef_adv_strontium_current", _strontium.value ?: 0.0)
        setInput("reef_strontium_current", _strontium.value ?: 0.0)
        
        setInput("reef_iodide_current", _iodide.value ?: 0.0)
        
        // Alkalinity handled by syncKhFromParams
    }
    
    fun resetAll() {
        _volume.value = 10.0
        _volumeUnit.value = "US"
        _volumeMode.value = "direct"
        _dimLength.value = 60.0
        _dimBreadth.value = 30.0
        _dimHeight.value = 40.0
        _dimUnit.value = "cm"
        _ammonia.value = 0.0
        _nitrite.value = 0.0
        _nitrate.value = 15.0
        _gh.value = 4.0
        _kh.value = 6.0
        _ph.value = 7.2
        _temperature.value = 26.0
        _salinity.value = 35.0
        _alkalinity.value = 8.0
        _calcium.value = 420.0
        _magnesium.value = 1300.0
        _phosphate.value = 0.05
        _dissolvedOxygen.value = 7.5
        _potassium.value = 15.0
        _iron.value = 0.1
        _strontium.value = 8.0
        _iodide.value = 0.06
        _ghUnit.value = "dh"
        _khUnit.value = "dh"
        _defaultWaterChangePercent.value = 20.0
        _aiInsight.value = AiInsightState()
        _chatMessages.value = emptyList()
        // Reset calc inputs
        _calcInputs.clear()
        
        applyProfileDefaults(_profile.value ?: AquariumProfile.FRESHWATER)
    }

    // ==================== Setters ====================

    fun setVolume(value: Double) { _volume.value = value }
    fun setVolumeUnit(unit: String) { _volumeUnit.value = unit }
    fun setVolumeMode(mode: String) { _volumeMode.value = mode }
    fun setDimLength(value: Double) { _dimLength.value = value }
    fun setDimBreadth(value: Double) { _dimBreadth.value = value }
    fun setDimHeight(value: Double) { _dimHeight.value = value }
    fun setDimUnit(unit: String) { _dimUnit.value = unit }
    
    fun setSubLength(value: Double) { _subLength.value = value }
    fun setSubWidth(value: Double) { _subWidth.value = value }
    fun setSubDepth(value: Double) { _subDepth.value = value }
    fun setSubProduct(index: Int) { _subProduct.value = index }
    fun setSubUnit(unit: String) { _subUnit.value = unit }

    // Salt Mix Inputs
    private val _saltMixProduct = MutableLiveData(0) // Index in list
    val saltMixProduct: LiveData<Int> = _saltMixProduct
    
    private val _saltMixVolume = MutableLiveData(5.0) // US Gallons default
    val saltMixVolume: LiveData<Double> = _saltMixVolume
    
    private val _saltMixCurrentPpt = MutableLiveData(0.0)
    val saltMixCurrentPpt: LiveData<Double> = _saltMixCurrentPpt
    
    private val _saltMixDesiredPpt = MutableLiveData(35.0)
    val saltMixDesiredPpt: LiveData<Double> = _saltMixDesiredPpt

    fun setSaltMixProduct(index: Int) { _saltMixProduct.value = index }
    fun setSaltMixVolume(value: Double) { _saltMixVolume.value = value }
    fun setSaltMixCurrentPpt(value: Double) { _saltMixCurrentPpt.value = value }
    fun setSaltMixDesiredPpt(value: Double) { _saltMixDesiredPpt.value = value }
    
    fun calculateSaltMix(): com.example.seachem_dosing.logic.SaltMixCalculations.SaltMixResult? {
        val products = com.example.seachem_dosing.logic.SaltMixCalculations.SALT_MIX_PRODUCTS.keys.toList()
        val index = _saltMixProduct.value ?: 0
        if (index !in products.indices) return null
        
        return com.example.seachem_dosing.logic.SaltMixCalculations.calculateSaltMix(
            products[index],
            _saltMixVolume.value ?: 0.0,
            _saltMixCurrentPpt.value ?: 0.0,
            _saltMixDesiredPpt.value ?: 0.0
        )
    }

    fun setProfile(profile: AquariumProfile) {
        _profile.value = profile
        applyProfileDefaults(profile)
    }
    
    private fun applyProfileDefaults(profile: AquariumProfile) {
        when (profile) {
            AquariumProfile.FRESHWATER -> {
                _ammonia.value = 0.0
                _nitrite.value = 0.0
                _nitrate.value = 15.0
                _gh.value = 4.0
                _kh.value = 6.0
                _ph.value = 7.2
                _temperature.value = 26.0
                _ghUnit.value = "dh"
                _khUnit.value = "dh"
                syncGhFromParams()
                syncKhFromParams()
                syncPhFromParams()
                syncNewParams()
            }
            AquariumProfile.SALTWATER -> {
                _ammonia.value = 0.0
                _nitrite.value = 0.0
                _gh.value = 0.0
                _kh.value = 0.0
                _dissolvedOxygen.value = 0.0
                _salinity.value = 35.0
                _alkalinity.value = 8.0
                _calcium.value = 420.0
                _magnesium.value = 1300.0
                _nitrate.value = 10.0
                _phosphate.value = 0.05
                _ph.value = 8.2
                _temperature.value = 26.0
                syncPhFromParams()
                syncNewParams()
            }
            AquariumProfile.POND -> {
                _ammonia.value = 0.0
                _nitrite.value = 0.0
                _nitrate.value = 20.0
                _gh.value = 0.0
                _kh.value = 5.0
                _ph.value = 7.4
                _temperature.value = 22.0
                _dissolvedOxygen.value = 7.5
                _khUnit.value = "dh"
                syncKhFromParams()
                syncPhFromParams()
            }
        }
    }
    
    // Parameter setters
    fun setAmmonia(value: Double) { _ammonia.value = value }
    fun setNitrite(value: Double) { _nitrite.value = value }
    fun setNitrate(value: Double) { _nitrate.value = value }
    fun setGh(value: Double) { _gh.value = value }
    fun setKh(value: Double) { _kh.value = value }
    fun setPh(value: Double) { _ph.value = value }
    fun setTemperature(value: Double) { _temperature.value = value }
    fun setSalinity(value: Double) { _salinity.value = value }
    fun setAlkalinity(value: Double) { _alkalinity.value = value }
    fun setCalcium(value: Double) { _calcium.value = value }
    fun setMagnesium(value: Double) { _magnesium.value = value }
    fun setPhosphate(value: Double) { _phosphate.value = value }
    fun setDissolvedOxygen(value: Double) { _dissolvedOxygen.value = value }
    fun setPotassium(value: Double) { _potassium.value = value }
    fun setIron(value: Double) { _iron.value = value }
    fun setStrontium(value: Double) { _strontium.value = value }
    fun setIodide(value: Double) { _iodide.value = value }
    fun setGhUnit(unit: String) { _ghUnit.value = unit }
    fun setKhUnit(unit: String) { _khUnit.value = unit }
    
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