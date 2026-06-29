package com.example.seachem_dosing.di

import com.example.seachem_dosing.BuildConfig
import com.example.seachem_dosing.domain.usecase.LogAdministeredDoseUseCase
import com.example.seachem_dosing.domain.usecase.RecordWaterParameterReadingUseCase
import org.koin.dsl.module

/**
 * Domain-layer DI bindings: the explicit history write-trigger use cases (factory-scoped, stateless).
 * The earlier Calculate/Convert/Validate use cases + CalculationsRepository were orphaned (no UI
 * caller — the calculator drives [com.example.seachem_dosing.ui.MainViewModel] directly) and removed.
 */
val domainModule = module {
    factory { LogAdministeredDoseUseCase(get(), { System.currentTimeMillis() }, BuildConfig.VERSION_NAME) }
    factory { RecordWaterParameterReadingUseCase(get(), { System.currentTimeMillis() }, BuildConfig.VERSION_NAME) }
}
