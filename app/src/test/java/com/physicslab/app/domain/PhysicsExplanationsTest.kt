package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsExplanationsTest {

    @Test
    fun getFor_returnsCorrectExplanationForEachQuantity() {
        assertEquals(Quantity.MAGNETIC_FIELD, PhysicsExplanations.getFor(Quantity.MAGNETIC_FIELD).quantity)
        assertEquals(Quantity.ACCELERATION, PhysicsExplanations.getFor(Quantity.ACCELERATION).quantity)
        assertEquals(Quantity.LIGHT, PhysicsExplanations.getFor(Quantity.LIGHT).quantity)
    }

    @Test
    fun magneticField_containsRequiredPhysicalFormulasAndNotes() {
        val mag = PhysicsExplanations.MAGNETIC_FIELD
        assertEquals("µT", mag.unitSymbol)
        assertTrue(mag.formulaText.contains("B = √(Bx² + By² + Bz²)"))
        assertTrue(mag.siUnit.contains("Tesla"))
        assertTrue(mag.siUnit.contains("µT"))

        // Reference range: Earth's field roughly 25 to 65 µT
        val earthRange = mag.referenceRanges.find { it.first.contains("Earth") }
        assertNotNull(earthRange)
        assertTrue(earthRange!!.second.contains("25 to 65 µT"))

        // Disturbances: magnets, steel, speakers, cases, electronics
        val dist = mag.disturbanceAndLimitations.lowercase()
        assertTrue(dist.contains("magnet"))
        assertTrue(dist.contains("steel"))
        assertTrue(dist.contains("speaker"))
        assertTrue(dist.contains("case"))
        assertTrue(dist.contains("recalibrate") || dist.contains("figure-8"))
    }

    @Test
    fun acceleration_containsFormulaAndGravityNotes() {
        val accel = PhysicsExplanations.ACCELERATION
        assertEquals("m/s²", accel.unitSymbol)
        assertTrue(accel.formulaText.contains("a = √(ax² + ay² + az²)"))

        // Gravity: ~9.81 m/s² at rest, free fall near 0
        val restRange = accel.referenceRanges.find { it.first.contains("Rest") }
        assertNotNull(restRange)
        assertTrue(restRange!!.second.contains("9.81"))

        val freeFallRange = accel.referenceRanges.find { it.first.contains("Free Fall") }
        assertNotNull(freeFallRange)
        assertTrue(freeFallRange!!.second.contains("0 m/s²"))
    }

    @Test
    fun light_containsIlluminanceUnitsAndConservativeRanges() {
        val light = PhysicsExplanations.LIGHT
        assertEquals("lux", light.unitSymbol)
        assertTrue(light.siUnit.contains("lumens per square meter") || light.siUnit.contains("lm/m²"))

        // Ranges: dim indoor, office, overcast, sunlight
        val ranges = light.referenceRanges.map { it.first }
        assertTrue(ranges.any { it.contains("Dim Indoor") })
        assertTrue(ranges.any { it.contains("Office") })
        assertTrue(ranges.any { it.contains("Overcast") })
        assertTrue(ranges.any { it.contains("Sunlight") })

        // Note: sensor uncalibrated and indicative
        val notes = (light.disturbanceAndLimitations + " " + light.sensorNote).lowercase()
        assertTrue(notes.contains("uncalibrated"))
        assertTrue(notes.contains("indicative") || notes.contains("approximate"))
    }

    @Test
    fun allList_containsAllThreeExplanations() {
        assertEquals(3, PhysicsExplanations.ALL.size)
    }
}
