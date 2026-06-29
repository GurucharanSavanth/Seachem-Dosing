package com.example.seachem_dosing.di

import androidx.room.Room
import com.example.seachem_dosing.data.local.database.AppDatabase
import com.example.seachem_dosing.data.local.database.MIGRATION_1_2
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryEventRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.UUID

/**
 * Data-layer DI bindings: Room database (v2 — ADR-011) + HistoryDao + repository implementations.
 * [MIGRATION_1_2] converts any legacy v1 history rows non-destructively on upgrade.
 */
val dataModule = module {

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DB_NAME,
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    single { get<AppDatabase>().historyDao() }

    single<HistoryEventRepository> { HistoryEventRepositoryImpl(get()) { UUID.randomUUID().toString() } }
}
