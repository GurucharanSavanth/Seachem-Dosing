package com.example.seachem_dosing.di

import com.example.seachem_dosing.data.repository.HistoryEventRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Static graph check: every `get<T>()` inside a definition has a provider.
 * Catches missing-binding errors at test time instead of app-startup time.
 *
 * Note: dataModule is excluded from static verify because Room.databaseBuilder
 * resolves androidContext() at runtime; that path is exercised by
 * androidTest/ integration tests once Phase 4.3 instrumented tests land.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinVerifyAllTest {

    @Test
    fun `appModule resolves`() {
        // HistoryEventRepository is provided at runtime by the (statically-excluded) dataModule.
        appModule.verify(extraTypes = listOf(HistoryEventRepository::class))
    }
}
