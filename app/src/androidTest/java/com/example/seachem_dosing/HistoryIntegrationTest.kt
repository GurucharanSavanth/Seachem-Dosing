package com.example.seachem_dosing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.data.local.database.AppDatabase
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryEventRepositoryImpl
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.usecase.LogAdministeredDoseUseCase
import com.example.seachem_dosing.domain.usecase.RecordWaterParameterReadingUseCase
import com.example.seachem_dosing.ui.dashboard.WaterReadingsRecorder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Phase 5 — end-to-end write→read through use cases + real Room (ADR-011 §11). */
@RunWith(AndroidJUnit4::class)
class HistoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: HistoryEventRepository
    private var counter = 0

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = HistoryEventRepositoryImpl(db.historyDao()) { "evt-${++counter}" }
    }

    @After fun teardown() = db.close()

    private fun recorder() = WaterReadingsRecorder(RecordWaterParameterReadingUseCase(repo, { 1000L }, "1.0"))

    @Test fun saveReadings_throughRecorder_appearInTimeline() = runBlocking {
        val saved = recorder().save("fw", 100.0, "batch-1", listOf(ParameterType.NITRATE to 15.0, ParameterType.PH to 7.4))
        assertEquals(2, saved)
        val rows = repo.observeTimeline("fw").first()
        assertEquals(2, rows.size)
        val nitrate = rows.first { it.parameter?.parameterTypeCode == "nitrate" }
        assertEquals("15.0", nitrate.parameter?.measuredValueDecimal)
        assertEquals("ppm_mg_per_l", nitrate.parameter?.measuredUnitCode)
        assertEquals("100.0", nitrate.parameter?.tankVolumeDecimal)
    }

    @Test fun duplicateBatch_isIdempotent_oneRecord() = runBlocking {
        recorder().save("fw", 100.0, "batch-x", listOf(ParameterType.NITRATE to 15.0))
        recorder().save("fw", 100.0, "batch-x", listOf(ParameterType.NITRATE to 15.0)) // same batch key
        assertEquals(1, repo.observeTimeline("fw").first().size)
    }

    @Test fun logAdministeredDose_appearsAsDoseAdministered() = runBlocking {
        LogAdministeredDoseUseCase(repo, { 2000L }, "1.0").invoke(
            idempotencyKey = "dose-1", aquariumProfileId = "fw", productId = "prime",
            administered = Quantity(StoredDecimal.parseNewValue("5.0"), UnitCode.MILLILITER),
            tankVolume = Quantity(StoredDecimal.parseNewValue("100"), UnitCode.LITER),
            engineVersion = "engine-1", userModifiedAmount = false,
        )
        val rows = repo.observeTimeline("fw").first()
        assertEquals(1, rows.size)
        assertEquals("dose_administered", rows[0].event.eventTypeCode)
        assertEquals("5.0", rows[0].dose?.administeredAmountDecimal)
    }

    @Test fun timeline_isProfileScoped() = runBlocking {
        recorder().save("fw", 100.0, "b1", listOf(ParameterType.NITRATE to 10.0))
        recorder().save("sw", 100.0, "b2", listOf(ParameterType.NITRATE to 20.0))
        assertEquals(1, repo.observeTimeline("fw").first().size)
        assertEquals(1, repo.observeTimeline("sw").first().size)
    }
}
