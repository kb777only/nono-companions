package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class WeatherRainTest {
    private fun reading(temp: Float,code: Int=0)=WeatherReading(1000,temp,code,0f)
    @Test fun temperatureUsesRealCelsiusWithHysteresis() {
        val w=WeatherState()
        w.update(reading(11.9f),1000,12); assertEquals(Outfit.COLD,w.outfit)
        w.update(reading(13.9f),1000,12); assertEquals(Outfit.COLD,w.outfit)
        w.update(reading(14f),1000,12); assertEquals(Outfit.DEFAULT,w.outfit)
        w.update(reading(26f),1000,12); assertEquals(Outfit.HOT,w.outfit)
        w.update(reading(24.1f),1000,12); assertEquals(Outfit.HOT,w.outfit)
        w.update(reading(23.9f),1000,12); assertEquals(Outfit.DEFAULT,w.outfit)
        assertEquals(23.9f,w.temperature!!,.001f)
    }
    @Test fun pyjamasWinAtNightEvenOfflineAndRainRemainsIndependent() {
        val w=WeatherState(); w.update(reading(-5f,61),1000,22)
        assertEquals(Outfit.NIGHT,w.outfit); assertTrue(w.raining)
        w.update(null,5000,6); assertEquals(Outfit.NIGHT,w.outfit); assertFalse(w.raining)
        w.update(reading(30f),1000,7); assertEquals(Outfit.HOT,w.outfit)
    }
    @Test fun staleInvalidAndFutureReadingsCannotDriveWeather() {
        for(r in listOf(reading(Float.NaN),reading(100f),WeatherReading(2000,10f,61,0f),WeatherReading(-20_000_000,10f,61,0f))) {
            val w=WeatherState(); w.update(r,1000,12)
            assertEquals(Outfit.DEFAULT,w.outfit); assertFalse(w.raining); assertNull(w.temperature)
        }
    }
    @Test fun rainCodesDoNotMistakeSnowForWater() {
        for(code in listOf(51,57,61,67,80,82,95,99)) assertTrue(reading(10f,code).raining)
        for(code in listOf(0,3,45,71,75,85,86)) assertFalse(reading(10f,code).raining)
        assertTrue(WeatherReading(1000,10f,3,.1f).raining)
    }
    @Test fun fallingWaterBouncesOnUmbrellaAndBody() {
        for(umbrella in listOf(true,false)) {
            val rain=RainParticles(Random(4)); rain.tick(1000,umbrella,false)
            val surface=rain.surface(.5f,umbrella)!!
            val drop=WaterDrop(.5f,surface-.01f,0f,1f,1000,1800)
            rain.drops.add(drop); rain.tick(1032,umbrella,false)
            assertTrue(drop.bounced); assertTrue(drop.vy<0f); assertTrue(drop.vx!=0f)
        }
    }
    @Test fun dropsFadeInOutAndNeverAccumulateWithoutBound() {
        val drop=WaterDrop(0f,0f,0f,1f,1000,1000)
        assertEquals(0f,drop.alpha(1000)); assertEquals(1f,drop.alpha(1200))
        assertTrue(drop.alpha(1900)<1); assertEquals(0f,drop.alpha(2000))
        val rain=RainParticles(Random(4))
        for(t in 1000L..60_000L step 32) { rain.tick(t,true,true,.01f); assertTrue(rain.drops.size<=60); assertTrue(rain.drops.all { t-it.born<it.life }) }
        rain.clear(); assertTrue(rain.drops.isEmpty())
    }
    @Test fun walkingLeavesDropletsBehindButStandingDoesNot() {
        val rain=RainParticles(Random(2)); rain.tick(1000,true,false,.1f)
        assertTrue(rain.drops.none { it.trail })
        rain.tick(1100,true,true,.1f); val trail=rain.drops.first { it.trail }; val x=trail.x
        rain.tick(1132,true,false,.2f); assertTrue(trail.x<x-.1f)
    }
    @Test fun aNearlyFullPoolDoesNotOverflowOnFootstep() {
        val rain=RainParticles(Random(4))
        repeat(59) { rain.drops.add(WaterDrop(.5f,.8f,0f,0f,1000,1000)) }
        rain.tick(1100,true,true,.01f)
        assertEquals(60,rain.drops.size)
    }
    @Test fun outfitMappingCoversAllFullBodyActions() {
        for(frame in 0..41) assertTrue(dressedFrame(frame) in 0..11)
        assertEquals(7,dressedFrame(38)); assertEquals(9,dressedFrame(40)); assertEquals(6,dressedFrame(30))
    }
}
