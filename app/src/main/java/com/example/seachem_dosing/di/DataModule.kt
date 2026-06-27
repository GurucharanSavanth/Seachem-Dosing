package com.example.seachem_dosing.di

import androidx.room.Room
import com.example.seachem_dosing.data.local.database.AppDatabase
import com.example.seachem_dosing.data.repository.CalculationsRepository
import com.example.seachem_dosing.data.repository.CalculationsRepositoryImpl
import com.example.seachem_dosing.data.repository.HistoryRepository
import com.example.seachem_dosing.data.repository.HistoryRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Data-layer DI bindings: Room database + DAOs + Repository implementations.
 *
 * SettingsRepository will be added here when its implementation lands
 * (Phase 4.7 StateFlow migration).
 */
val dataModule = module {

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).build()
    }

    single { get<AppDatabase>().parameterLogDao() }
    single { get<AppDatabase>().dosingLogDao() }

    single<HistoryRepository> { HistoryRepositoryImpl(get(), get()) }
    single<CalculationsRepository> { CalculationsRepositoryImpl() }
}
