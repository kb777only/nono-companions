package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class KissAndGestureTest {
    private fun world()=World(Random(9)).apply {
        resize(400f,700f,100f,150f,0)
        pets.forEachIndexed { i,p -> p.x=80f+i*150; p.y=550f; p.state=State.IDLE; p.motion.grounded=true }
    }
    @Test fun stationaryHoldOpensOnceAndSuppressesTap() {
        val g=TouchHold(12f,500); g.down(1000)
        assertFalse(g.move(5f,5f)); assertFalse(g.hold(1499)); assertTrue(g.hold(1500))
        assertFalse(g.hold(1700)); assertFalse(g.end()); assertFalse(g.hold(2000))
    }
    @Test fun dragWinsBeforeAndAfterLongPress() {
        for(openFirst in listOf(false,true)) {
            val g=TouchHold(12f,500); g.down(1000)
            if(openFirst) assertTrue(g.hold(1500))
            assertTrue(g.move(10f,10f)); assertFalse(g.hold(1800)); assertFalse(g.end())
        }
    }
    @Test fun smallMotionStillTapsAndNextGestureResets() {
        val g=TouchHold(12f,500); g.down(1000); g.move(20f,0f); g.end()
        g.down(2000); assertFalse(g.move(3f,4f)); assertTrue(g.end())
    }
    @Test fun kissHasContactHeartsAndReleasesBothRoles() {
        for(who in Who.entries) {
            val w=world(); assertTrue(w.choose(who,1,1000)); val seen=mutableSetOf<KissHeart>()
            for(t in 1000L..10000L step 32) {
                w.tick(t); seen.addAll(w.hearts)
                assertTrue(w.hearts.all { t-it.born<it.life && it.life in 400..900 })
                if(w.interaction?.stage==2) assertTrue(w.pets.all { w.pose(it.who,t)==Pose.KISS })
            }
            assertTrue(seen.size>8); assertEquals(Who.entries.toSet(),seen.map { it.who }.toSet())
            assertTrue(seen.map { it.x }.distinct().size>4); assertTrue(seen.map { it.life }.distinct().size>4)
            assertTrue(w.hearts.isEmpty()); assertNull(w.interaction); assertTrue(w.bond>25)
        }
    }
    @Test fun interruptedKissClearsHeartsImmediately() {
        val w=world(); w.choose(Who.HUSBAND,1,1000)
        for(t in 1000L..4000L step 32) { w.tick(t); if(w.hearts.isNotEmpty()) break }
        assertTrue(w.hearts.isNotEmpty()); w.command(Command.Drag(Who.WIFE,40f,50f),4100)
        assertTrue(w.hearts.isEmpty()); assertNull(w.interaction)
    }
    @Test fun menuCannotStartAirborneKissAndRestIsExplicit() {
        val w=world(); w.pets[0].y=80f
        assertFalse(w.choose(Who.WIFE,1,1000)); assertNull(w.interaction)
        assertTrue(w.choose(Who.WIFE,3,5000)); assertEquals(State.RESTING,w.pet(Who.WIFE).state)
    }
}
