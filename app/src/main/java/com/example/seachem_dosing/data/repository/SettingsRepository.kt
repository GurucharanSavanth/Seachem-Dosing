package com.example.seachem_dosing.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persistent app preferences (theme, language, default water-change %, etc.).
 *
 * Backed by DataStore Preferences in `SettingsRepositoryImpl` (added when
 * Phase 4.7 StateFlow migration replaces SavedStateHandle-keyed preferences
 * in [com.example.seachem_dosing.ui.MainViewModel]).
 */
interface SettingsRepository {

    val themeMode: Flow<ThemeMode>
    val languageCode: Flow<String>
    val volumeUnit: Flow<String>
    val hardnessUnit: Flow<String>
    val defaultWaterChangePercent: Flow<Double>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguageCode(code: String)
    suspend fun setVolumeUnit(unit: String)
    suspend fun setHardnessUnit(unit: String)
    suspend fun setDefaultWaterChangePercent(percent: Double)

    enum class ThemeMode { SYSTEM, LIGHT, DARK }
}
