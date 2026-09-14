package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DeviceHeatTest {
    @Test fun strictlyAbove43WithOneDegreeHysteresisAndNoRounding() {
        val heat=DeviceHeat()
        heat.sample(430); assertFalse(heat.hot); assertEquals(43f,heat.celsius!!,0f)
        heat.sample(431); assertTrue(heat.hot); assertEquals(43.1f,heat.celsius!!,.001f)
        heat.sample(430); assertTrue(heat.hot)
        heat.sample(421); assertTrue(heat.hot)
        heat.sample(420); assertFalse(heat.hot)
    }
    @Test fun invalidOrUnavailableSensorClearsOverride() {
        for(value in listOf(null,Int.MIN_VALUE,1001,-401)) {
            val heat=DeviceHeat(); heat.sample(450); heat.sample(value)
            assertNull(heat.celsius); assertFalse(heat.hot)
        }
    }
    @Test fun heatWinsOverColdAndNightButDoesNotEraseRainOrWeatherTemperature() {
        val w=WeatherState(); val reading=WeatherReading(1000,-3.5f,61,.2f)
        w.update(reading,1000,23,true)
        assertEquals(Outfit.HOT,w.outfit); assertTrue(w.raining)
        assertEquals(-3.5f,w.temperature!!,0f)
        w.update(reading,1000,23,false); assertEquals(Outfit.NIGHT,w.outfit)
        w.update(reading,1000,12,false); assertEquals(Outfit.COLD,w.outfit)
        assertFalse(w.line(Who.HUSBAND).contains("écharpe"))
    }
    @Test fun fanIsPlacedBeforeItIsSwitchedOnAndThenPutAway() {
        val c=CoolingController(); c.tick(true,true,1000)
        assertEquals(CoolingPhase.FANNING,c.phase); assertFalse(c.deskVisible)
        c.tick(true,true,7000); assertEquals(CoolingPhase.PLACING,c.phase)
        assertTrue(c.deskVisible); assertFalse(c.spinning)
        c.tick(true,true,7900); assertEquals(CoolingPhase.SWITCHING,c.phase); assertFalse(c.spinning)
        c.tick(true,true,8600); assertTrue(c.spinning)
        c.tick(true,true,17600); assertEquals(CoolingPhase.QUIET,c.phase); assertFalse(c.deskVisible)
        c.tick(true,true,24600); assertEquals(CoolingPhase.FANNING,c.phase)
    }
    @Test fun interruptionClearsPropsAndEnforcesRetryDelayWithoutCatchingUp() {
        val c=CoolingController(); c.tick(true,true,1000); c.tick(true,true,7000)
        c.tick(true,false,7100); assertEquals(CoolingPhase.NONE,c.phase); assertFalse(c.deskVisible)
        c.tick(true,true,10099); assertEquals(CoolingPhase.NONE,c.phase)
        c.tick(true,true,10100); assertEquals(CoolingPhase.FANNING,c.phase)
        c.tick(true,true,1_000_000); assertEquals(CoolingPhase.PLACING,c.phase)
        c.tick(false,true,1_000_001); assertFalse(c.spinning); assertFalse(c.deskVisible)
    }
    private fun hotWorld(): World {
        val w=World(Random(8)); w.resize(400f,700f,72f,104f,1000)
        w.pets.forEach { it.y=596f; it.state=State.IDLE; it.motion.grounded=true }
        w.environment(null,1000,23,1000); w.batteryTemperature(450,1000)
        w.tick(5000)
        return w
    }
    @Test fun dragAndKeyboardCancelCoolingAndPreserveTheirOwnPriority() {
        val w=hotWorld(); assertEquals(State.COOLING,w.pet(Who.HUSBAND).state)
        assertEquals(Pose.FAN,w.pose(Who.HUSBAND,5000)); assertEquals(Outfit.HOT,w.weather.outfit)
        w.command(Command.Drag(Who.HUSBAND,100f,100f),5100); w.tick(5200)
        assertEquals(State.DRAGGED,w.pet(Who.HUSBAND).state)
        assertEquals(CoolingPhase.NONE,w.cooling[0].phase)
        w.keyboard(true,5300); w.tick(5400)
        assertTrue(w.pets.all { it.state in listOf(State.RETREATING,State.PEEKING) })
        assertTrue(w.cooling.all { it.phase==CoolingPhase.NONE })
    }
    @Test fun coolingEndsAndNightOutfitReturnsWhenBatteryCools() {
        val w=hotWorld(); w.batteryTemperature(420,5100)
        assertEquals(Outfit.NIGHT,w.weather.outfit)
        assertTrue(w.pets.all { it.state==State.IDLE })
        assertTrue(w.cooling.all { it.phase==CoolingPhase.NONE })
    }
    @Test fun releaseFallsBeforeAnyCoolingPoseAndResizeDropsAllFans() {
        val w=hotWorld(); w.command(Command.Drag(Who.HUSBAND,100f,100f),5100)
        w.command(Command.Release(Who.HUSBAND),5200); w.tick(5300)
        assertEquals(Pose.FALL,w.pose(Who.HUSBAND,5300))
        assertEquals(CoolingPhase.NONE,w.cooling[0].phase)
        w.resize(700f,400f,72f,104f,5400)
        assertTrue(w.cooling.all { !it.deskVisible })
    }
}
