package com.physicslab.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.physicslab.app.data.AppDatabase
import com.physicslab.app.data.SensorReading
import com.physicslab.app.data.SensorReadingDao
import com.physicslab.app.data.StatsResult
import com.physicslab.app.domain.LikeEscaper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying [SensorReadingDao] operations against an in-memory database.
 *
 * NOTE: Requires an Android device or emulator to execute.
 * Marked as UNVERIFIED in headless CLI environments without an active emulator.
 */
@RunWith(AndroidJUnit4::class)
class SensorReadingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SensorReadingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.sensorReadingDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById_retrievesSameEntity() = runBlocking {
        val reading = SensorReading(
            id = 0L,
            quantity = "ACCELERATION",
            name = "Drop Test 1",
            timestamp = 1000L,
            x = 0.0,
            y = 9.8,
            z = 0.0,
            value = 9.8,
            sampleCount = 50,
            sensorName = "Linear Accel",
            accuracy = 3,
            updatedAt = 1000L
        )
        val id = dao.insert(reading)
        val fetched = dao.getById(id)

        assertNotNull(fetched)
        assertEquals("Drop Test 1", fetched?.name)
        assertEquals(9.8, fetched?.value ?: 0.0, 1e-9)
    }

    @Test
    fun renameAndObserve_updatesRow() = runBlocking {
        val reading = SensorReading(
            id = 0L,
            quantity = "MAGNETIC_FIELD",
            name = "Initial Name",
            timestamp = 2000L,
            x = 3.0,
            y = 4.0,
            z = 0.0,
            value = 5.0,
            sampleCount = 10,
            sensorName = "Magnetometer",
            accuracy = 2,
            updatedAt = 2000L
        )
        val id = dao.insert(reading)
        val rowsUpdated = dao.updateName(id, "Renamed Magnet Run", 3000L)
        assertEquals(1, rowsUpdated)

        val fetched = dao.getById(id)
        assertEquals("Renamed Magnet Run", fetched?.name)
        assertEquals(3000L, fetched?.updatedAt)
    }

    @Test
    fun deleteById_removesRow() = runBlocking {
        val reading = SensorReading(
            id = 0L,
            quantity = "LIGHT",
            name = "Ambient Light",
            timestamp = 3000L,
            x = null,
            y = null,
            z = null,
            value = 350.0,
            sampleCount = 20,
            sensorName = "Lux Sensor",
            accuracy = 3,
            updatedAt = 3000L
        )
        val id = dao.insert(reading)
        dao.deleteById(id)

        val fetched = dao.getById(id)
        assertNull(fetched)
    }

    @Test
    fun searchByName_withEscapedWildcard_matchesLiteralsOnly() = runBlocking {
        dao.insert(
            SensorReading(
                id = 0L,
                quantity = "MAGNETIC_FIELD",
                name = "Reading 100% calibrated",
                timestamp = 1000L,
                x = 1.0,
                y = 1.0,
                z = 1.0,
                value = 1.73,
                sampleCount = 10,
                sensorName = "Mag",
                accuracy = 3,
                updatedAt = 1000L
            )
        )
        dao.insert(
            SensorReading(
                id = 0L,
                quantity = "MAGNETIC_FIELD",
                name = "Reading 1000 uncalibrated",
                timestamp = 2000L,
                x = 1.0,
                y = 1.0,
                z = 1.0,
                value = 1.73,
                sampleCount = 10,
                sensorName = "Mag",
                accuracy = 3,
                updatedAt = 2000L
            )
        )

        // Search for literal "100%"
        val pattern = LikeEscaper.buildContainsPattern("100%")
        val results = dao.searchByName("MAGNETIC_FIELD", pattern).first()

        assertEquals(1, results.size)
        assertEquals("Reading 100% calibrated", results[0].name)
    }

    @Test
    fun observeStats_computesCorrectAggregates_andTransitionsToEmptyOnDelete() = runBlocking {
        // Empty initially
        val rawStatsInitial = dao.observeStats("ACCELERATION").first()
        val statsInitial = StatsResult.from(rawStatsInitial)
        assertTrue(statsInitial is StatsResult.Empty)

        // Insert readings
        dao.insert(
            SensorReading(
                id = 0L,
                quantity = "ACCELERATION",
                name = "A1",
                timestamp = 1000L,
                x = 0.0,
                y = 10.0,
                z = 0.0,
                value = 10.0,
                sampleCount = 5,
                sensorName = "Acc",
                accuracy = 3,
                updatedAt = 1000L
            )
        )
        dao.insert(
            SensorReading(
                id = 0L,
                quantity = "ACCELERATION",
                name = "A2",
                timestamp = 2000L,
                x = 0.0,
                y = 20.0,
                z = 0.0,
                value = 20.0,
                sampleCount = 5,
                sensorName = "Acc",
                accuracy = 3,
                updatedAt = 2000L
            )
        )

        val rawStatsPopulated = dao.observeStats("ACCELERATION").first()
        val statsPopulated = StatsResult.from(rawStatsPopulated) as StatsResult.Populated
        assertEquals(2, statsPopulated.count)
        assertEquals(10.0, statsPopulated.min, 1e-9)
        assertEquals(20.0, statsPopulated.max, 1e-9)
        assertEquals(15.0, statsPopulated.average, 1e-9)
        assertEquals(20.0, statsPopulated.latest, 1e-9) // newest timestamp is 2000L

        // Delete all
        dao.deleteAllByQuantity("ACCELERATION")
        val rawStatsAfterDelete = dao.observeStats("ACCELERATION").first()
        val statsAfterDelete = StatsResult.from(rawStatsAfterDelete)
        assertTrue(statsAfterDelete is StatsResult.Empty)
    }
}
