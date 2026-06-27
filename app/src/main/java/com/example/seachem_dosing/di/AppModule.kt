package com.example.seachem_dosing.di

import org.koin.dsl.module

/**
 * App-scoped DI bindings: settings managers, theme/locale state.
 *
 * Repository bindings live in [dataModule]; UseCase bindings will live in
 * domainModule (added at Phase 4.6). ViewModel bindings will be wired here
 * when Phase 4.7 (StateFlow migration) replaces the manually-instantiated
 * MainViewModel with Koin's `viewModel { }` DSL.
 */
val appModule = module {
    // Empty — populated as Phase 4.5+ (Repository) and 4.6 (UseCase) land.
}
