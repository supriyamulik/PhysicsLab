package com.physicslab.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun migration_1_2_hasCorrectVersionRange() {
        val migration = AppDatabase.MIGRATION_1_2
        assertNotNull(migration)
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }
}
