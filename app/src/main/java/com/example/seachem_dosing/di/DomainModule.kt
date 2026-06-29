package com.example.seachem_dosing.di

import com.example.seachem_dosing.domain.usecase.CalculateDoseUseCase
import com.example.seachem_dosing.domain.usecase.CalculateQuickDoseUseCase
import com.example.seachem_dosing.domain.usecase.ConvertUnitsUseCase
import com.example.seachem_dosing.domain.usecase.ValidateInputUseCase
import org.koin.dsl.module

/**
 * Domain-layer DI bindings: UseCase classes.
 * UseCases are factory-scoped (new instance per request) — they are stateless
 * and cheap to construct, no need to share across consumers.
 */
val domainModule = module {
    factory { CalculateDoseUseCase(get()) }
    factory { CalculateQuickDoseUseCase(get()) }
    factory { ConvertUnitsUseCase() }
    factory { ValidateInputUseCase() }
}
