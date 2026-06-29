package com.example.seachem_dosing.di

import com.example.seachem_dosing.ui.history.HistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App-scoped DI bindings. ViewModels that need repository/use-case injection are bound here via
 * Koin's `viewModel { }` DSL; the legacy [com.example.seachem_dosing.ui.MainViewModel] remains
 * AndroidX-instantiated (SavedStateHandle) until its StateFlow migration.
 */
val appModule = module {
    viewModel { HistoryViewModel(get()) }
}
