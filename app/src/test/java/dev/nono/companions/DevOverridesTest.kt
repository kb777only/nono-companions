package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class DevOverridesTest {
    @Test fun preciseFrenchTemperatureInputValidatesBoundaries() {
        assertEquals(431,DevVariable.DEVICE_TEMP.parse("43,1"))
        assertEquals(-125,DevVariable.WEATHER_TEMP.parse("-12.5"))
        assertNull(DevVariable.DEVICE_TEMP.parse("NaN"))
        assertNull(DevVariable.DEVICE_TEMP.parse("81"))
        assertNull(DevVariable.HOUR.parse("12.5"))
        assertEquals(23,DevVariable.HOUR.parse("23"))
    }
    @Test fun autoPassesThroughCurrentRealValues() {
        val d=DevOverrides(emptyMap<String,Int>())
        assertEquals(381,d.battery(381)); assertEquals(22,d.hour(22))
        assertNull(d.kind); assertNull(d.temperature); assertNull(d.gravity); assertNull(d.keyboard)
    }
    @Test fun independentOverridesRoundTripAndReturningAutoUsesLatestSensor() {
        val saved=mapOf("DEVICE_TEMP" to 451,"HOUR" to 3,"WEATHER_KIND" to 3)
        val d=DevOverrides(saved)
        assertEquals(451,d.battery(320)); assertEquals(3,d.hour(12)); assertEquals(WeatherKind.RAIN,d.kind)
        assertNull(d.temperature)
        assertEquals(390,DevOverrides(saved-"DEVICE_TEMP").battery(390))
    }
    @Test fun malformedPreferencesFallBackToAuto() {
        val d=DevOverrides(mapOf("HOUR" to 24,"ORIENTATION" to -1,"DEVICE_TEMP" to "450","WEATHER_KIND" to 7,"KEYBOARD" to 2))
        DevVariable.entries.forEach { assertNull(d[it]) }
    }
    @Test fun rainCanBeSimulatedWithoutInventingOutdoorTemperature() {
        val w=WeatherState(); w.update(null,1000,12,false,WeatherKind.RAIN,null)
        assertTrue(w.raining); assertNull(w.temperature); assertEquals(Outfit.DEFAULT,w.outfit)
        w.update(null,2000,12,false,null,8f)
        assertFalse(w.raining); assertEquals(Outfit.COLD,w.outfit); assertEquals(WeatherKind.UNKNOWN,w.kind)
    }
    @Test fun manualWeatherKindReplacesRealRainWithoutLosingRealTemperature() {
        val w=WeatherState(); w.update(WeatherReading(1000,9f,61,3f),1000,12,false,WeatherKind.CLEAR)
        assertFalse(w.raining); assertEquals(9f,w.temperature!!,0f)
    }
    @Test fun heatStillWinsOverManualColdAndNightAndAutoClearsAll() {
        val w=World(); w.environment(null,1000,23,1000,WeatherKind.RAIN,2f)
        w.batteryTemperature(450,1000); assertEquals(Outfit.HOT,w.weather.outfit); assertTrue(w.weather.raining)
        w.environment(null,2000,12,2000); w.batteryTemperature(350,2000)
        assertEquals(Outfit.DEFAULT,w.weather.outfit); assertFalse(w.weather.raining)
    }
    @Test fun quarterTurnsHaveMatchingGravityAndSmoothShortestRotation() {
        val expected=listOf(GravityVector(0f,1f),GravityVector(-1f,0f),GravityVector(0f,-1f),GravityVector(1f,0f))
        for(i in 0..3) {
            val d=DevOverrides(mapOf("ORIENTATION" to i)); val g=d.gravity!!
            assertEquals(expected[i].x,g.x,.001f); assertEquals(expected[i].y,g.y,.001f)
        }
        val snap=OrientationSnap(); assertTrue(snap.turnTo(90f,1000)); assertEquals(0f,snap.value(1000),.001f)
        assertTrue(snap.value(1200) in 1f..89f); assertEquals(90f,snap.value(1500),.001f)
        assertFalse(snap.turnTo(90f,1600)); assertFalse(snap.turnTo(Float.NaN,1600))
    }
    @Test fun keyboardAndContextAreIndependentAndValidated() {
        val d=DevOverrides(mapOf("KEYBOARD" to 0,"CONTEXT" to 2))
        assertEquals(false,d.keyboard); assertEquals(ContextSignal.GAME,d.context)
        assertNull(d.angle); assertNull(d.kind)
    }
}
