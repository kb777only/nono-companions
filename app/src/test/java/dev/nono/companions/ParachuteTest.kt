package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class ParachuteTest {
    @Test fun draggingPartnerDoesNotRestartAnOpenParachute() {
        val w=World(); w.resize(1000f,5000f,100f,150f,0)
        w.pet(Who.HUSBAND).apply { state=State.PARACHUTING; motion.fallSince=1000 }
        w.command(Command.Drag(Who.WIFE,50f,50f),2000)
        assertEquals(State.PARACHUTING,w.pet(Who.HUSBAND).state)
        assertEquals(1000L,w.pet(Who.HUSBAND).motion.fallSince)
        w.interrupt(2100,resetMotion=true)
        assertTrue(w.pets.none { it.state==State.PARACHUTING })
    }
    private fun falling()=Pet(Who.HUSBAND,100f,10f,state=State.FALLING).apply { motion.fallSince=1000 }
    @Test fun deploysAt650MillisecondsAndCapsDescent() {
        val physics=GravityPhysics(); val p=falling()
        physics.step(p,1000f,5000f,100f,150f,.01f,1649)
        assertEquals(State.FALLING,p.state)
        p.motion.vy=900f
        physics.step(p,1000f,5000f,100f,150f,.01f,1650)
        assertEquals(State.PARACHUTING,p.state)
        assertTrue(p.motion.vy<=150f*.85f+.01f)
        val y=p.y
        physics.step(p,1000f,5000f,100f,150f,.5f,2150)
        assertEquals(63.75f,p.y-y,.1f)
    }
    @Test fun draggingNeverDeploysAndReleaseStartsFreshTimer() {
        val w=World(); w.resize(1000f,5000f,100f,150f,0)
        w.command(Command.Drag(Who.WIFE,100f,100f),1000)
        for(t in 1000L..3000L step 100) w.tick(t)
        assertEquals(State.DRAGGED,w.pet(Who.WIFE).state)
        w.command(Command.Release(Who.WIFE),3000)
        w.tick(3649); assertEquals(State.FALLING,w.pet(Who.WIFE).state)
        w.tick(3650); assertEquals(State.PARACHUTING,w.pet(Who.WIFE).state)
        assertEquals(Pose.PARACHUTE,w.pose(Who.WIFE,3650))
        w.command(Command.Drag(Who.WIFE,100f,100f),3700)
        assertEquals(Pose.FALL,w.pose(Who.WIFE,3700)); assertEquals(-1L,w.pet(Who.WIFE).motion.fallSince)
    }
    @Test fun landingAndDisplayInterruptionRemoveGlideState() {
        val physics=GravityPhysics(); val p=falling(); p.y=540f
        physics.step(p,400f,700f,100f,150f,.3f,2000)
        assertTrue(p.motion.grounded); assertEquals(State.RECOVERING,p.state); assertEquals(-1L,p.motion.fallSince)
        val w=World(); w.pets[0].state=State.PARACHUTING; w.pets[0].motion.fallSince=10
        w.resize(300f,400f,72f,104f,1000)
        assertFalse(w.pets.any { it.state==State.PARACHUTING }); assertEquals(-1L,w.pets[0].motion.fallSince)
    }
    @Test fun glideSpeedCapFollowsTiltAndEachCharacterHasOwnTimer() {
        val physics=GravityPhysics(); physics.setGravity(1f,0f)
        val a=falling(); val b=falling().copy(who=Who.WIFE,motion=Motion(fallSince=1500))
        a.motion.vx=900f; b.motion.vx=900f
        physics.step(a,5000f,5000f,100f,150f,.01f,1700)
        physics.step(b,5000f,5000f,100f,150f,.01f,1700)
        assertEquals(State.PARACHUTING,a.state); assertEquals(State.FALLING,b.state)
        assertTrue(a.motion.vx<=127.51f); assertTrue(b.motion.vx>127.51f)
    }
}
