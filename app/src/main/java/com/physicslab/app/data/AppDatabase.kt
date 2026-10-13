package com.physicslab.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single Room database for the entire app.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ Version history                                                          │
 * │  1  Initial: `readings` table (legacy ReadingLogger data).              │
 * │  2  Adds `sensor_readings` table for the new domain layer.              │
 * │     Legacy `readings` rows whose sensorType matches a known quantity    │
 * │     (MAGNETIC_FIELD, ACCELERATION, LIGHT) are copied in with sensible   │
 * │     defaults so no existing user data is lost.                          │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Legacy → sensor_readings column mapping:
 *
 * | sensor_readings column | Source                                           |
 * |------------------------|--------------------------------------------------|
 * | id                     | AUTOINCREMENT (new key)                          |
 * | quantity               | Derived from `readings.sensorType`:              |
 * |                        |   "MAGNETIC_FIELD" → MAGNETIC_FIELD              |
 * |                        |   "ACCELERATION"   → ACCELERATION                |
 * |                        |   "LIGHT"          → LIGHT                       |
 * |                        |   Other rows are NOT migrated.                   |
 * | name                   | `readings.label`                                 |
 * | timestamp              | `readings.timestamp`                             |
 * | x, y, z               | NULL  (legacy table stored only a scalar value)  |
 * | value                  | `readings.value`                                 |
 * | sampleCount            | 1     (legacy captured single-shot readings)     |
 * | sensorName             | `readings.label` (best available approximation) |
 * | accuracy               | NULL  (legacy table did not record accuracy)     |
 * | updatedAt              | `readings.timestamp`                             |
 *
 * Only sensorType values 'MAGNETIC_FIELD', 'ACCELERATION', and 'LIGHT' are
 * migrated. All other legacy rows (GYROSCOPE, BAROMETER, etc.) remain in the
 * `readings` table unchanged for display in the legacy HistoryActivity.
 */
@Database(
    entities  = [ReadingEntity::class, SensorReading::class],
    version   = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /** Legacy DAO used by the existing sensor Activities and HistoryActivity. */
    abstract fun readingDao(): ReadingDao

    /** New domain-layer DAO for the three targeted quantities. */
    abstract fun sensorReadingDao(): SensorReadingDao

    companion object {

        @Volatile private var INSTANCE: AppDatabase? = null

        // ── Migrations ───────────────────────────────────────────────────────

        /**
         * 1 → 2: Creates the `sensor_readings` table, then copies compatible
         * legacy rows from `readings` (sensorType IN ['MAGNETIC_FIELD',
         * 'ACCELERATION', 'LIGHT']) into it. The legacy `readings` table is
         * left completely unchanged so existing user data survives.
         *
         * SQL column types follow SQLite affinity rules used by Room:
         *   INTEGER NOT NULL  →  Long / Int (non-nullable)
         *   REAL              →  Double?    (nullable)
         *   TEXT NOT NULL     →  String     (non-nullable)
         *   INTEGER           →  Int?       (nullable)
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new sensor_readings table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sensor_readings (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        quantity    TEXT    NOT NULL,
                        name        TEXT    NOT NULL,
                        timestamp   INTEGER NOT NULL,
                        x           REAL,
                        y           REAL,
                        z           REAL,
                        value       REAL    NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        sensorName  TEXT    NOT NULL,
                        accuracy    INTEGER,
                        updatedAt   INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // 2. Copy compatible legacy rows from 'readings' into 'sensor_readings'.
                //    Only the three quantities this app tracks are migrated.
                //    x, y, z → NULL  (legacy had no component breakdown)
                //    sampleCount → 1 (legacy captured single-shot readings)
                //    accuracy → NULL (legacy table did not record sensor accuracy status)
                db.execSQL(
                    """
                    INSERT INTO sensor_readings
                        (quantity, name, timestamp, x, y, z, value, sampleCount, sensorName, accuracy, updatedAt)
                    SELECT
                        sensorType,         -- already matches Quantity.name for the three known types
                        label,              -- user-visible label stored in legacy table
                        timestamp,
                        NULL,               -- x: not available in legacy table
                        NULL,               -- y: not available in legacy table
                        NULL,               -- z: not available in legacy table
                        value,
                        1,                  -- sampleCount: legacy was single-shot
                        label,              -- sensorName: best available approximation
                        NULL,               -- accuracy: not tracked in legacy table
                        timestamp           -- updatedAt equals insert time
                    FROM readings
                    WHERE sensorType IN ('MAGNETIC_FIELD', 'ACCELERATION', 'LIGHT')
                    """.trimIndent()
                )
            }
        }

        // ── Singleton ────────────────────────────────────────────────────────

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "physicslab.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
