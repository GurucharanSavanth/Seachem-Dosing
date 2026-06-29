package com.example.seachem_dosing.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.seachem_dosing.data.local.database.AppDatabase
import com.example.seachem_dosing.data.local.entity.DoseEventDetailEntity
import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.ParameterEventDetailEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** ADR-011 Gate D — transactional DAO behaviour against a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HistoryDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.historyDao()
    }

    @After fun teardown() = db.close()

    private fun event(id: String, type: String, profile: String = "fw", at: Long = 1000, idem: String = id) =
        HistoryEventEntity(
            eventId = id, eventTypeCode = type, aquariumProfileId = profile,
            occurredAtEpochMillis = at, createdAtEpochMillis = at, sourceModuleCode = "test",
            idempotencyKey = idem, schemaVersion = 2, precisionStatusCode = "new_exact",
        )

    private fun dose(id: String) = DoseEventDetailEntity(
        eventId = id, productId = "prime", tankVolumeDecimal = "100", tankVolumeUnitCode = "l",
        administeredAmountDecimal = "5.0", administeredAmountUnitCode = "ml", userModifiedAmount = false,
    )

    @Test fun insertDoseEvent_isAtomic_andQueryableInTimeline() = runBlocking {
        dao.insertDoseEvent(event("e1", "dose_administered"), dose("e1"))
        assertEquals("e1", dao.findEventIdByIdempotencyKey("e1"))
        val rows = dao.observeTimeline("fw").first()
        assertEquals(1, rows.size)
        assertEquals("e1", rows[0].event.eventId)
        assertEquals("5.0", rows[0].dose?.administeredAmountDecimal)
        assertNull(rows[0].parameter)
    }

    @Test fun insertParameterEvent_isAtomic_andQueryable() = runBlocking {
        val detail = ParameterEventDetailEntity(
            eventId = "p1", parameterTypeCode = "nitrate", measuredValueDecimal = "15",
            measuredUnitCode = "ppm_mg_per_l", validationStatusCode = "validated",
        )
        dao.insertParameterEvent(event("p1", "water_parameter_recorded"), detail)
        val rows = dao.observeTimeline("fw").first()
        assertEquals(1, rows.size)
        assertEquals("15", rows[0].parameter?.measuredValueDecimal)
    }

    @Test fun timeline_isNewestFirst_andProfileScoped() = runBlocking {
        dao.insertDoseEvent(event("a", "dose_administered", at = 1000, idem = "a"), dose("a"))
        dao.insertDoseEvent(event("b", "dose_administered", at = 3000, idem = "b"), dose("b"))
        dao.insertDoseEvent(event("c", "dose_administered", profile = "sw", at = 2000, idem = "c"), dose("c"))
        val fw = dao.observeTimeline("fw").first()
        assertEquals(listOf("b", "a"), fw.map { it.event.eventId }) // newest first, sw excluded
    }

    @Test fun observeByType_filters() = runBlocking {
        dao.insertDoseEvent(event("d", "dose_administered", idem = "d"), dose("d"))
        dao.insertParameterEvent(
            event("p", "water_parameter_recorded", idem = "p"),
            ParameterEventDetailEntity("p", "ph", "7.4", "ph_value", validationStatusCode = "validated"),
        )
        val doses = dao.observeByType("fw", "dose_administered").first()
        assertEquals(1, doses.size)
        assertEquals("d", doses[0].event.eventId)
    }

    @Test fun voidEvent_recordsVoidsTarget_andShowsInVoidedIds() = runBlocking {
        dao.insertDoseEvent(event("orig", "dose_administered", idem = "orig"), dose("orig"))
        dao.insertVoidEvent(event("v", "void", idem = "v").copy(voidsEventId = "orig", correctionReason = "dup"))
        assertEquals(listOf("orig"), dao.observeVoidedEventIds("fw").first())
    }

    @Test fun findByIdempotencyKey_missing_returnsNull() = runBlocking {
        assertNull(dao.findEventIdByIdempotencyKey("nope"))
    }
}
