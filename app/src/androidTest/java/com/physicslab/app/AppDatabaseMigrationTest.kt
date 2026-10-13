package com.physicslab.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.physicslab.app.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for Room migration 1 → 2 using [MigrationTestHelper].
 *
 * Requires: exported schema JSON at app/schemas/ (enabled via kapt arg in build.gradle).
 *
 * Run with: ./gradlew assembleDebugAndroidTest  (compiles)
 *            ./gradlew connectedDebugAndroidTest  (executes on device/emulator)
 *
 * MigrationTestHelper opens the database at version 1 using the exported schema JSON,
 * so the v1 table structure is validated by Room itself — not hand-coded SQL.
 *
 * Test coverage
 * ─────────────
 * 1. v1 `readings` rows whose sensorType ∈ {MAGNETIC_FIELD, ACCELERATION, LIGHT}
 *    are copied into `sensor_readings` with sensible defaults.
 * 2. Other legacy rows (e.g. GYROSCOPE) are NOT copied into sensor_readings.
 * 3. The original `readings` table is preserved intact.
 * 4. All non-null columns in sensor_readings satisfy their constraints.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration1to2_copiesCompatibleLegacyRows() {
        // ── Phase 1: create v1 database ──────────────────────────────────────
        val db = helper.createDatabase("migration_test", 1)

        // Insert one row per sensorType: two that SHOULD be migrated, two that should not
        db.execSQL(
            "INSERT INTO readings (sensorType, label, value, unit, timestamp) " +
                "VALUES ('MAGNETIC_FIELD', 'Magnetic reading', 42.5, 'µT', 1700000000000)"
        )
        db.execSQL(
            "INSERT INTO readings (sensorType, label, value, unit, timestamp) " +
                "VALUES ('ACCELERATION', 'Accel reading', 9.81, 'm/s²', 1700000001000)"
        )
        db.execSQL(
            "INSERT INTO readings (sensorType, label, value, unit, timestamp) " +
                "VALUES ('LIGHT', 'Light reading', 350.0, 'lx', 1700000002000)"
        )
        db.execSQL(
            "INSERT INTO readings (sensorType, label, value, unit, timestamp) " +
                "VALUES ('GYROSCOPE', 'Gyro reading', 0.05, 'rad/s', 1700000003000)"
        )
        db.close()

        // ── Phase 2: apply migration ─────────────────────────────────────────
        val migratedDb = helper.runMigrationsAndValidate(
            "migration_test",
            2,
            true,
            AppDatabase.MIGRATION_1_2
        )

        // ── Phase 3: verify sensor_readings contents ─────────────────────────
        val cursor = migratedDb.query(
            "SELECT quantity, name, value, sampleCount, accuracy, x, y, z " +
                "FROM sensor_readings ORDER BY value ASC"
        )

        // Expect exactly 3 rows (MAGNETIC_FIELD + ACCELERATION + LIGHT); GYROSCOPE excluded
        assertEquals("Exactly 3 known-quantity rows should be migrated", 3, cursor.count)

        val quantities = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val qIdx  = cursor.getColumnIndexOrThrow("quantity")
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val valIdx  = cursor.getColumnIndexOrThrow("value")
            val scIdx   = cursor.getColumnIndexOrThrow("sampleCount")
            val accIdx  = cursor.getColumnIndexOrThrow("accuracy")
            val xIdx    = cursor.getColumnIndexOrThrow("x")

            quantities.add(cursor.getString(qIdx))
            // name should equal the legacy label
            assertTrue("name must not be blank", cursor.getString(nameIdx).isNotEmpty())
            // value must be positive/non-zero for our test data
            assertTrue("value must be > 0", cursor.getDouble(valIdx) > 0.0)
            // sampleCount must be 1 (default for legacy single-shot rows)
            assertEquals("sampleCount must default to 1", 1, cursor.getInt(scIdx))
            // accuracy must be NULL (legacy table had no accuracy field)
            assertTrue("accuracy must be NULL for migrated legacy rows", cursor.isNull(accIdx))
            // x must be NULL (legacy table had no component breakdown)
            assertTrue("x must be NULL for migrated legacy rows", cursor.isNull(xIdx))
        }
        cursor.close()

        assertTrue("MAGNETIC_FIELD must be in migrated rows", quantities.contains("MAGNETIC_FIELD"))
        assertTrue("ACCELERATION must be in migrated rows",   quantities.contains("ACCELERATION"))
        assertTrue("LIGHT must be in migrated rows",          quantities.contains("LIGHT"))

        // ── Phase 4: original `readings` table must be untouched ─────────────
        val legacyCursor = migratedDb.query("SELECT COUNT(*) FROM readings")
        legacyCursor.moveToFirst()
        assertEquals("All 4 legacy rows must still exist", 4, legacyCursor.getInt(0))
        legacyCursor.close()

        // ── Phase 5: GYROSCOPE must NOT appear in sensor_readings ─────────────
        val gyroCursor = migratedDb.query(
            "SELECT COUNT(*) FROM sensor_readings WHERE quantity='GYROSCOPE'"
        )
        gyroCursor.moveToFirst()
        assertEquals("GYROSCOPE rows must not be migrated", 0, gyroCursor.getInt(0))
        gyroCursor.close()

        migratedDb.close()
    }
}
