package com.example.seachem_dosing.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.seachem_dosing.ai.AiInsightState
import com.example.seachem_dosing.ai.ChatMessage
import com.example.seachem_dosing.ai.ChatRole
import com.example.seachem_dosing.logic.Calculations

/**
 * Main ViewModel for the Seachem Dosing app.
 * Manages all water parameters, volume settings, and calculation results.
 */
class MainViewModel : ViewModel() {

    enum class AquariumProfile(val id: String) {
        FRESHWATER("freshwater"),
        SALTWATER("saltwater"),
        POND("pond");

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

    private val aquariumKeywords = setOf(
        "aquarium", "fish", "tank", "dosing", "gh", "kh", "ph", "ammonia", "nitrite", "nitrate",
        "salinity", "alkalinity", "calcium", "magnesium", "phosphate", "pond", "reef", "freshwater",
        "saltwater", "water change", "prime", "stability", "safe", "buffer", "filter", "cycle",
        "bioload", "ppm", "dgh", "dkh", "seachem", "plant", "substrate", "oxygen", "co2"
    )

    // ==================== Volume Settings ====================

    private val _volume = MutableLiveData(10.0)
    val volume: LiveData<Double> = _volume

    private val _volumeUnit = MutableLiveData("US") // L, US, UK
    val volumeUnit: LiveData<String> = _volumeUnit

    private val _volumeMode = MutableLiveData("direct") // direct or lbh
    val volumeMode: LiveData<String> = _volumeMode

    // Dimensions for L×B×H mode
    private val _dimLength = MutableLiveData(60.0)
    val dimLength: LiveData<Double> = _dimLength

    private val _dimBreadth = MutableLiveData(30.0)
    val dimBreadth: LiveData<Double> = _dimBreadth

    private val _dimHeight = MutableLiveData(40.0)
    val dimHeight: LiveData<Double> = _dimHeight

    private val _dimUnit = MutableLiveData("cm") // cm, in, ft
    val dimUnit: LiveData<String> = _dimUnit

    // ==================== Water Parameters ====================

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

    private val _ghUnit = MutableLiveData("dh") // dh or ppm
    val ghUnit: LiveData<String> = _ghUnit

    private val _khUnit = MutableLiveData("dh") // dh or ppm
    val khUnit: LiveData<String> = _khUnit

    // ==================== Calculator Inputs ====================

    // KH Booster (KHCO3)
    private val _khCurrent = MutableLiveData(0.0)
    val khCurrent: LiveData<Double> = _khCurrent

    private val _khTarget = MutableLiveData(4.0)
    val khTarget: LiveData<Double> = _khTarget

    private val _khPurity = MutableLiveData(0.99)
    val khPurity: LiveData<Double> = _khPurity

    // GH Booster (Equilibrium)
    private val _ghCurrent = MutableLiveData(0.0)
    val ghCurrent: LiveData<Double> = _ghCurrent

    private val _ghTarget = MutableLiveData(6.0)
    val ghTarget: LiveData<Double> = _ghTarget

    // Neutral Regulator
    private val _phCurrent = MutableLiveData(7.5)
    val phCurrent: LiveData<Double> = _phCurrent

    private val _phTarget = MutableLiveData(7.0)
    val phTarget: LiveData<Double> = _phTarget

    private val _nrKh = MutableLiveData(4.0)
    val nrKh: LiveData<Double> = _nrKh

    // Acid Buffer
    private val _acidCurrentKh = MutableLiveData(6.0)
    val acidCurrentKh: LiveData<Double> = _acidCurrentKh

    private val _acidTargetKh = MutableLiveData(4.0)
    val acidTargetKh: LiveData<Double> = _acidTargetKh

    // Gold Buffer
    private val _goldPhCurrent = MutableLiveData(7.0)
    val goldPhCurrent: LiveData<Double> = _goldPhCurrent

    private val _goldPhTarget = MutableLiveData(7.5)
    val goldPhTarget: LiveData<Double> = _goldPhTarget

    // ==================== Computed Values ====================

    /**
     * Get effective volume in litres based on current mode
     */
    val effectiveVolumeLitres: LiveData<Double> = MutableLiveData<Double>().apply {
        // This will be recalculated when inputs change
    }

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

    /**
     * Get GH value in degrees (converts from ppm if needed)
     */
    fun getGhInDegrees(): Double {
        val value = _gh.value ?: 0.0
        return if (_ghUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }

    /**
     * Get KH value in degrees (converts from ppm if needed)
     */
    fun getKhInDegrees(): Double {
        val value = _kh.value ?: 0.0
        return if (_khUnit.value == "ppm") Calculations.ppmToDh(value) else value
    }

    // ==================== Status Results ====================

    data class ParameterStatus(
        val status: Status,
        val displayValue: String
    )

    enum class Status { GOOD, WARNING, DANGER, INFO }

    fun getAmmoniaStatus(): ParameterStatus {
        val value = _ammonia.value ?: 0.0
        return ParameterStatus(
            if (value > 0) Status.DANGER else Status.GOOD,
            "${value.format(1)} ppm"
        )
    }

    fun getNitriteStatus(): ParameterStatus {
        val value = _nitrite.value ?: 0.0
        return ParameterStatus(
            if (value > 0) Status.DANGER else Status.GOOD,
            "${value.format(1)} ppm"
        )
    }

    fun getNitrateStatus(): ParameterStatus {
        val value = _nitrate.value ?: 0.0
        return ParameterStatus(
            if (value > 50) Status.WARNING else Status.GOOD,
            "${value.format(0)} ppm"
        )
    }

    fun getGhStatus(): ParameterStatus {
        val dh = getGhInDegrees()
        return ParameterStatus(
            Status.INFO,
            "${dh.format(1)} °dGH"
        )
    }

    fun getKhStatus(): ParameterStatus {
        val dh = getKhInDegrees()
        return ParameterStatus(
            Status.INFO,
            "${dh.format(1)} °dKH"
        )
    }

    // ==================== Calculation Results ====================

    data class DosingResult(
        val dose: Double,
        val unit: String,
        val splitDose: String = ""
    )

    fun calculateKhco3(): DosingResult {
        val litres = getEffectiveVolumeLitres()
        val grams = Calculations.calculateKhco3Grams(
            _khCurrent.value ?: 0.0,
            _khTarget.value ?: 0.0,
            litres,
            _khPurity.value ?: 0.99
        )
        return DosingResult(grams, "g", getSplitText(grams))
    }

    fun calculateEquilibrium(): DosingResult {
        val litres = getEffectiveVolumeLitres()
        val deltaGh = (_ghTarget.value ?: 0.0) - (_ghCurrent.value ?: 0.0)
        val grams = if (deltaGh > 0) Calculations.calculateEquilibriumGrams(deltaGh, litres) else 0.0
        return DosingResult(grams, "g", getSplitText(grams))
    }

    fun calculateSafe(): DosingResult {
        val litres = getEffectiveVolumeLitres()
        val grams = Calculations.calculateSafeGrams(litres)
        return DosingResult(grams, "g")
    }

    fun calculateAptComplete(): Calculations.AptResult {
        val litres = getEffectiveVolumeLitres()
        return Calculations.calculateAptCompleteDose(litres, _nitrate.value ?: 0.0)
    }

    fun calculateNeutralRegulator(): DosingResult {
        val litres = getEffectiveVolumeLitres()
        val grams = Calculations.calculateNeutralRegulatorGrams(
            litres,
            _phCurrent.value ?: 7.0,
            _phTarget.value ?: 7.0,
            _nrKh.value ?: 4.0
        )
        return DosingResult(grams, "g", getSplitText(grams))
    }

    fun calculateAcidBuffer(): DosingResult {
        val litres = getEffectiveVolumeLitres()
        val grams = Calculations.calculateAcidBufferGrams(
            litres,
            _acidCurrentKh.value ?: 0.0,
            _acidTargetKh.value ?: 0.0
        )
        return DosingResult(grams, "g", getSplitText(grams))
    }

    fun calculateGoldBuffer(): Pair<DosingResult, Boolean> {
        val litres = getEffectiveVolumeLitres()
        val result = Calculations.calculateGoldBufferGrams(
            litres,
            _goldPhCurrent.value ?: 7.0,
            _goldPhTarget.value ?: 7.5
        )
        return Pair(DosingResult(result.grams, "g", getSplitText(result.grams)), result.fullDose)
    }

    fun calculatePrimeDose(): Double {
        return Calculations.calculatePrimeDose(getEffectiveVolumeLitres())
    }

    fun calculateStabilityDose(): Double {
        return Calculations.calculateStabilityDose(getEffectiveVolumeLitres())
    }

    fun calculateWaterChangeLitres(percent: Double): Double {
        if (percent <= 0) return 0.0
        val litres = getEffectiveVolumeLitres()
        return litres * (percent / 100.0)
    }

    fun requestAiInsight() {
        _aiInsight.value = AiInsightState(
            isLoading = false,
            text = null,
            error = "AI disabled for now."
        )
    }

    fun sendChatMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return
        appendChatMessage(ChatMessage(ChatRole.USER, trimmed))

        if (!isAquariumTopic(trimmed)) {
            appendChatMessage(
                ChatMessage(
                    ChatRole.ASSISTANT,
                    "I can only help with aquarium, fish, and dosing questions."
                )
            )
            return
        }
        appendChatMessage(
            ChatMessage(
                ChatRole.ASSISTANT,
                "AI disabled for now."
            )
        )
    }

    // ==================== Setters ====================

    fun setVolume(value: Double) { _volume.value = value }
    fun setVolumeUnit(unit: String) { _volumeUnit.value = unit }
    fun setVolumeMode(mode: String) { _volumeMode.value = mode }
    fun setDimLength(value: Double) { _dimLength.value = value }
    fun setDimBreadth(value: Double) { _dimBreadth.value = value }
    fun setDimHeight(value: Double) { _dimHeight.value = value }
    fun setDimUnit(unit: String) { _dimUnit.value = unit }

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
    fun setGhUnit(unit: String) { _ghUnit.value = unit }
    fun setKhUnit(unit: String) { _khUnit.value = unit }
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

    fun setProfile(profile: AquariumProfile) {
        _profile.value = profile
        applyProfileDefaults(profile)
    }

    // ==================== Sync Methods ====================

    /**
     * Sync KH value from parameters to calculator inputs
     */
    fun syncKhFromParams() {
        val khDegrees = getKhInDegrees()
        _khCurrent.value = khDegrees
        _nrKh.value = khDegrees
        _acidCurrentKh.value = khDegrees
    }

    /**
     * Sync GH value from parameters to calculator inputs
     */
    fun syncGhFromParams() {
        val ghDegrees = getGhInDegrees()
        _ghCurrent.value = ghDegrees
    }

    /**
     * Sync pH value from parameters to calculator inputs
     */
    fun syncPhFromParams() {
        val currentPh = _ph.value ?: 7.0
        _phCurrent.value = currentPh
        _goldPhCurrent.value = currentPh
    }

    // ==================== Utility ====================

    private fun getSplitText(grams: Double): String {
        if (grams <= 0.01) return ""
        if (grams < 0.3) return "Dose in one go"
        val half = grams / 2
        return "${half.format(2)}g now + ${half.format(2)}g in 12-24h"
    }

    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

    private fun convertHardness(value: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> value
            fromUnit == "ppm" && toUnit == "dh" -> Calculations.ppmToDh(value)
            fromUnit == "dh" && toUnit == "ppm" -> Calculations.dhToPpm(value)
            else -> value
        }
    }

    private fun buildInsightSystemPrompt(context: String): String {
        return """
            You are an aquarium assistant. Only answer aquarium, fish, dosing, and water-parameter questions.
            Give concise, actionable guidance. Avoid medical or unrelated topics. If readings are missing,
            ask for them. Do not invent dosing formulas; reference the app's calculators instead.
            Double-check recommendations against the provided readings before responding.

            Readings:
            $context
        """.trimIndent()
    }

    private fun buildChatPrompt(): String {
        val context = buildDashboardContext()
        return """
            Use the latest dashboard readings as context. If the user asks about dosing or parameters,
            respond with safe, conservative guidance and mention relevant app calculators.
            If the question is unclear, ask a clarifying question.

            Latest readings:
            $context
        """.trimIndent()
    }

    private fun buildDashboardContext(): String {
        val profile = _profile.value ?: AquariumProfile.FRESHWATER
        val unit = _volumeUnit.value ?: "L"
        val ghUnit = _ghUnit.value ?: "dh"
        val khUnit = _khUnit.value ?: "dh"
        val lines = mutableListOf<String>()
        lines.add("Profile: ${profile.name.lowercase().replaceFirstChar { it.uppercase() }}")
        lines.add("Effective volume: ${"%.1f".format(getEffectiveVolumeLitres())} L (unit: $unit)")
        lines.add("GH unit: $ghUnit, KH unit: $khUnit")
        when (profile) {
            AquariumProfile.FRESHWATER -> {
                lines.add("Ammonia: ${_ammonia.value ?: 0.0} ppm")
                lines.add("Nitrite: ${_nitrite.value ?: 0.0} ppm")
                lines.add("Nitrate: ${_nitrate.value ?: 0.0} ppm")
                lines.add("GH input: ${_gh.value ?: 0.0} $ghUnit (dGH: ${"%.2f".format(getGhInDegrees())})")
                lines.add("KH input: ${_kh.value ?: 0.0} $khUnit (dKH: ${"%.2f".format(getKhInDegrees())})")
                lines.add("pH: ${_ph.value ?: 0.0}")
                lines.add("Temperature: ${_temperature.value ?: 0.0} C")
            }
            AquariumProfile.SALTWATER -> {
                lines.add("Salinity: ${_salinity.value ?: 0.0} ppt")
                lines.add("Alkalinity: ${_alkalinity.value ?: 0.0} dKH")
                lines.add("Calcium: ${_calcium.value ?: 0.0} ppm")
                lines.add("Magnesium: ${_magnesium.value ?: 0.0} ppm")
                lines.add("Nitrate: ${_nitrate.value ?: 0.0} ppm")
                lines.add("Phosphate: ${_phosphate.value ?: 0.0} ppm")
                lines.add("pH: ${_ph.value ?: 0.0}")
                lines.add("Temperature: ${_temperature.value ?: 0.0} C")
            }
            AquariumProfile.POND -> {
                lines.add("Ammonia: ${_ammonia.value ?: 0.0} ppm")
                lines.add("Nitrite: ${_nitrite.value ?: 0.0} ppm")
                lines.add("Nitrate: ${_nitrate.value ?: 0.0} ppm")
                lines.add("KH input: ${_kh.value ?: 0.0} $khUnit (dKH: ${"%.2f".format(getKhInDegrees())})")
                lines.add("pH: ${_ph.value ?: 0.0}")
                lines.add("Temperature: ${_temperature.value ?: 0.0} C")
                lines.add("Dissolved oxygen: ${_dissolvedOxygen.value ?: 0.0} mg/L")
            }
        }
        return lines.joinToString(separator = "\n")
    }

    private fun isAquariumTopic(message: String): Boolean {
        val lower = message.lowercase()
        return aquariumKeywords.any { lower.contains(it) }
    }

    private fun appendChatMessage(message: ChatMessage) {
        val current = _chatMessages.value.orEmpty()
        val updated = (current + message).takeLast(20)
        _chatMessages.value = updated
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
        _ghUnit.value = "dh"
        _khUnit.value = "dh"
        _khCurrent.value = 0.0
        _khTarget.value = 4.0
        _khPurity.value = 0.99
        _ghCurrent.value = 0.0
        _ghTarget.value = 6.0
        _phCurrent.value = 7.5
        _phTarget.value = 7.0
        _nrKh.value = 4.0
        _acidCurrentKh.value = 6.0
        _acidTargetKh.value = 4.0
        _goldPhCurrent.value = 7.0
        _goldPhTarget.value = 7.5
        _aiInsight.value = AiInsightState()
        _chatMessages.value = emptyList()
        applyProfileDefaults(_profile.value ?: AquariumProfile.FRESHWATER)
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
}
