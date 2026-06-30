package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.R
import com.example.seachem_dosing.logic.Calculations
import java.util.Locale

/**
 * Pure dashboard recommendation logic, extracted from DashboardFragment (ADR-001
 * prep). No Android Context: returns structured [Report] of string-resource IDs +
 * pre-formatted numeric values; the UI maps IDs → strings. This makes the
 * thresholds unit-testable (SPEC §V — deterministic, no UI).
 *
 * Threshold values + message mapping are a faithful 1:1 port of the former
 * updateFreshwater/Saltwater/PondRecommendations — do not "improve" them without
 * a parity test, or the dashboard advice silently changes.
 */
object RecommendationEngine {

    /** An actionable suggestion: a string resource + optional pre-formatted arg. */
    data class Msg(val res: Int, val arg: String? = null)

    /** A detail line. [Param] = labelled reading + status; [Plain] = a single string. */
    sealed interface Line {
        data class Param(val labelRes: Int, val value: String, val unitRes: Int, val msgRes: Int) : Line
        data class Plain(val res: Int) : Line
    }

    data class Report(val actions: List<Msg>, val details: List<Line>)

    data class FreshwaterInput(
        val litres: Double, val ammonia: Double, val nitrite: Double, val nitrate: Double,
        val ghDegrees: Double, val khDegrees: Double, val ph: Double, val temp: Double,
        val potassium: Double, val iron: Double,
    )

    data class SaltwaterInput(
        val litres: Double, val salinity: Double, val alkalinity: Double, val calcium: Double,
        val magnesium: Double, val nitrate: Double, val phosphate: Double, val ph: Double,
        val temp: Double, val strontium: Double, val iodide: Double,
    )

    private fun fmt(value: Double, decimals: Int): String = String.format(Locale.ROOT, "%.${decimals}f", value)

    fun freshwater(i: FreshwaterInput): Report {
        val actions = mutableListOf<Msg>()
        if (i.litres <= 0) actions += Msg(R.string.reco_action_volume_needed)
        if (i.ammonia > 0) actions += if (i.litres > 0)
            Msg(R.string.reco_action_ammonia, fmt(Calculations.calculatePrimeDose(i.litres), 1))
        else Msg(R.string.reco_action_ammonia_generic)
        if (i.nitrite > 0) actions += if (i.litres > 0)
            Msg(R.string.reco_action_nitrite, fmt(Calculations.calculateStabilityDose(i.litres), 1))
        else Msg(R.string.reco_action_nitrite_generic)
        if (i.nitrate > 50) actions += if (i.litres > 0)
            Msg(R.string.reco_action_nitrate_high, fmt(i.litres * 0.30, 1))
        else Msg(R.string.reco_action_nitrate_high_generic)
        if (i.khDegrees < 3) actions += Msg(R.string.reco_action_kh_low)
        if (i.potassium < 10) actions += Msg(R.string.reco_action_potassium_low)
        if (i.iron < 0.05) actions += Msg(R.string.reco_action_iron_low)

        val details = listOf(
            Line.Param(R.string.param_ammonia, fmt(i.ammonia, 2), R.string.unit_ppm,
                if (i.ammonia > 0) R.string.reco_msg_ammonia_high else R.string.reco_msg_ammonia_ok),
            Line.Param(R.string.param_nitrite, fmt(i.nitrite, 2), R.string.unit_ppm,
                if (i.nitrite > 0) R.string.reco_msg_nitrite_high else R.string.reco_msg_nitrite_ok),
            Line.Param(R.string.param_nitrate, fmt(i.nitrate, 0), R.string.unit_ppm,
                when { i.nitrate > 50 -> R.string.reco_msg_nitrate_high; i.nitrate > 20 -> R.string.reco_msg_nitrate_moderate; else -> R.string.reco_msg_nitrate_low }),
            Line.Param(R.string.param_gh, fmt(i.ghDegrees, 1), R.string.unit_dgh_short,
                when { i.ghDegrees > 12 -> R.string.reco_msg_gh_high; i.ghDegrees < 3 -> R.string.reco_msg_gh_low; else -> R.string.reco_msg_gh_ok }),
            Line.Param(R.string.param_kh, fmt(i.khDegrees, 1), R.string.unit_dkh_short,
                when { i.khDegrees > 10 -> R.string.reco_msg_kh_high; i.khDegrees < 3 -> R.string.reco_msg_kh_low; else -> R.string.reco_msg_kh_ok }),
            Line.Param(R.string.param_ph, fmt(i.ph, 2), R.string.unit_ph,
                when { i.ph < 6.5 -> R.string.reco_msg_ph_low; i.ph > 8.2 -> R.string.reco_msg_ph_high; else -> R.string.reco_msg_ph_ok }),
            Line.Param(R.string.param_potassium, fmt(i.potassium, 0), R.string.unit_mg_l,
                if (i.potassium < 15) R.string.reco_msg_potassium_low else R.string.reco_msg_potassium_ok),
            Line.Param(R.string.param_iron, fmt(i.iron, 2), R.string.unit_mg_l,
                if (i.iron < 0.1) R.string.reco_msg_iron_low else R.string.reco_msg_iron_ok),
            Line.Param(R.string.param_temp, fmt(i.temp, 1), R.string.unit_temp_c,
                when { i.temp < 22 -> R.string.reco_msg_temp_low; i.temp > 30 -> R.string.reco_msg_temp_high; else -> R.string.reco_msg_temp_ok }),
        )
        return Report(actions, details)
    }

    fun saltwater(i: SaltwaterInput): Report {
        val actions = mutableListOf<Msg>()
        if (i.litres <= 0) actions += Msg(R.string.reco_action_volume_needed)
        if (i.nitrate > 20) actions += if (i.litres > 0)
            Msg(R.string.reco_action_nitrate_reef_high, fmt(i.litres * 0.20, 1))
        else Msg(R.string.reco_action_nitrate_reef_high_generic)
        if (i.phosphate > 0.1) actions += if (i.litres > 0)
            Msg(R.string.reco_action_phosphate_high, fmt(i.litres * 0.15, 1))
        else Msg(R.string.reco_action_phosphate_high_generic)
        if (i.strontium < 8) actions += Msg(R.string.reco_action_strontium_low)
        if (i.iodide < 0.06) actions += Msg(R.string.reco_action_iodide_low)

        val details = listOf(
            Line.Param(R.string.param_salinity, fmt(i.salinity, 1), R.string.unit_salinity_ppt,
                when { i.salinity > 36 -> R.string.reco_msg_salinity_high; i.salinity < 33 -> R.string.reco_msg_salinity_low; else -> R.string.reco_msg_salinity_ok }),
            Line.Param(R.string.param_alkalinity, fmt(i.alkalinity, 1), R.string.unit_dkh_short,
                when { i.alkalinity > 11 -> R.string.reco_msg_alkalinity_high; i.alkalinity < 7 -> R.string.reco_msg_alkalinity_low; else -> R.string.reco_msg_alkalinity_ok }),
            Line.Param(R.string.param_calcium, fmt(i.calcium, 0), R.string.unit_calcium_ppm,
                when { i.calcium > 450 -> R.string.reco_msg_calcium_high; i.calcium < 380 -> R.string.reco_msg_calcium_low; else -> R.string.reco_msg_calcium_ok }),
            Line.Param(R.string.param_magnesium, fmt(i.magnesium, 0), R.string.unit_magnesium_ppm,
                when { i.magnesium > 1450 -> R.string.reco_msg_magnesium_high; i.magnesium < 1200 -> R.string.reco_msg_magnesium_low; else -> R.string.reco_msg_magnesium_ok }),
            Line.Param(R.string.param_nitrate, fmt(i.nitrate, 1), R.string.unit_ppm,
                when { i.nitrate > 20 -> R.string.reco_msg_nitrate_reef_high; i.nitrate > 5 -> R.string.reco_msg_nitrate_reef_moderate; else -> R.string.reco_msg_nitrate_reef_low }),
            Line.Param(R.string.param_phosphate, fmt(i.phosphate, 3), R.string.unit_phosphate_ppm,
                when { i.phosphate > 0.1 -> R.string.reco_msg_phosphate_high; i.phosphate < 0.03 -> R.string.reco_msg_phosphate_low; else -> R.string.reco_msg_phosphate_ok }),
            Line.Param(R.string.param_ph, fmt(i.ph, 2), R.string.unit_ph,
                when { i.ph > 8.5 -> R.string.reco_msg_ph_salt_high; i.ph < 7.8 -> R.string.reco_msg_ph_salt_low; else -> R.string.reco_msg_ph_salt_ok }),
            Line.Param(R.string.param_strontium, fmt(i.strontium, 0), R.string.unit_mg_l,
                if (i.strontium < 8) R.string.reco_msg_strontium_low else R.string.reco_msg_strontium_ok),
            Line.Param(R.string.param_iodide, fmt(i.iodide, 2), R.string.unit_mg_l,
                if (i.iodide < 0.06) R.string.reco_msg_iodide_low else R.string.reco_msg_iodide_ok),
            Line.Param(R.string.param_temp, fmt(i.temp, 1), R.string.unit_temp_c,
                when { i.temp > 27 -> R.string.reco_msg_temp_salt_high; i.temp < 24 -> R.string.reco_msg_temp_salt_low; else -> R.string.reco_msg_temp_salt_ok }),
        )
        return Report(actions, details)
    }

    /** Utility/"Sand and Gravel" profile: volume-only action + a single description line. */
    fun pond(litres: Double): Report {
        val actions = if (litres <= 0) listOf(Msg(R.string.reco_action_volume_needed)) else emptyList()
        return Report(actions, listOf(Line.Plain(R.string.profile_pond_desc)))
    }
}
