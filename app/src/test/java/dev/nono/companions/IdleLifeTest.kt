package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class IdleLifeTest {
    private fun world()=World(Random(8)).apply {
        resize(400f,700f,100f,150f,0)
        pets.forEach { it.state=State.IDLE; it.y=550f; it.motion.grounded=true }
        pets[0].x=90f; pets[1].x=180f
    }
    @Test fun allEightSoloPerformancesEnterAnimateFinishAndCoolDown() {
        for(who in Who.entries) for(antic in IdleAntic.entries) {
            val w=world(); assertTrue(w.startIdle(who,antic,1000))
            assertEquals(Pose.IDLE,w.pose(who,1000))
            assertEquals(antic.pose,w.pose(who,1600))
            for(t in 1000L..(1000+antic.duration+100) step 50) w.tick(t)
            assertNull(w.idleLife[who.ordinal].performance)
            assertEquals(State.IDLE,w.pet(who).state)
            assertFalse(w.startIdle(who,antic,1000+antic.duration+200))
            assertNull(w.interaction)
        }
    }
    @Test fun runningActuallyMovesAndStaysInsideBounds() {
        val w=world(); assertTrue(w.startIdle(Who.HUSBAND,IdleAntic.RUN,1000))
        var maxX=90f
        for(t in 1000L..5300L step 40) {
            w.tick(t); maxX=maxOf(maxX,w.pets[0].x)
            assertTrue(w.pets.all { it.x in 0f..300f && it.y in 0f..550f })
        }
        assertTrue(maxX>130f)
    }
    @Test fun quietAndTiredChoicesNeverRunOrStretch() {
        val c=IdleLife()
        repeat(100) {
            assertFalse(c.choose(Needs(energy=10f),false,1000,Random(it))!!.active)
            assertFalse(c.choose(Needs(),true,1000,Random(it))!!.active)
        }
    }
    @Test fun everyInterruptionClearsSoloWithoutStalePose() {
        for(antic in IdleAntic.entries) for(cause in 0..4) {
            val w=world(); assertTrue(w.startIdle(Who.WIFE,antic,1000)); w.tick(1500)
            when(cause) {
                0 -> w.command(Command.Drag(Who.WIFE,50f,30f),1600)
                1 -> w.keyboard(true,1600)
                2 -> w.resize(300f,500f,100f,150f,1600)
                3 -> w.batteryTemperature(450,1600)
                else -> w.gravity(1f,0f,1600)
            }
            assertNull(w.idleLife[Who.WIFE.ordinal].performance)
            assertNotEquals(antic.pose,w.pose(Who.WIFE,1700))
        }
    }
    @Test fun sharedScenesAcknowledgeBeforePartnerJoinsThenReconcile() {
        for(kind in listOf(Kind.FOOT_DUET,Kind.COPY_STRETCH,Kind.TAG)) for(leader in Who.entries) {
            val w=world(); assertTrue(w.start(kind,leader,1000))
            var acknowledgment=false; var together=false
            for(t in 1000L..19000L step 50) {
                w.tick(t)
                if(w.interaction?.stage==1) {
                    acknowledgment=true
                    assertEquals(Pose.IDLE,w.pose(leader.partner(),t))
                }
                if(w.interaction?.stage==2 && t-w.interaction!!.since>700) {
                    together=true
                    assertEquals(w.pose(leader,t),w.pose(leader.partner(),t))
                }
                assertTrue(w.pets.all { it.x in 0f..300f && it.y in 0f..550f })
                if(w.interaction==null) break
            }
            assertTrue(acknowledgment); assertTrue(together)
            assertNull(w.interaction); assertTrue(w.bond>25f)
            assertFalse(w.start(kind,leader,19001))
        }
    }
    @Test fun allNewClipsAdvanceAndInterrupt() {
        for(pose in listOf(Pose.FEET,Pose.STRETCH,Pose.SIGNATURE,Pose.RUN)) {
            val p=AnimationPlayer()
            val frames=(1000L..5000L step 50).map { p.frame(pose,it) }.toSet()
            assertTrue(frames.size>=4)
            assertTrue(frames.all { it in pose.frame..pose.frame+3 })
            assertEquals(Pose.FALL.frame,p.frame(Pose.FALL,5100))
        }
    }
    @Test fun jointInteractionPreemptsSoloAndCoolingPreventsEntry() {
        val w=world(); assertTrue(w.startIdle(Who.HUSBAND,IdleAntic.SIGNATURE,1000))
        assertTrue(w.start(Kind.DINO,Who.WIFE,1500)); assertNull(w.idleLife[0].performance)
        w.interrupt(2000); w.pets.forEach { it.state=State.IDLE }
        w.batteryTemperature(450,3000)
        assertFalse(w.startIdle(Who.HUSBAND,IdleAntic.FEET,15000))
        assertFalse(w.start(Kind.TAG,Who.HUSBAND,15000))
    }
}
