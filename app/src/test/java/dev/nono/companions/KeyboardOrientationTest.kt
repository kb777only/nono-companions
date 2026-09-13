package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class KeyboardOrientationTest {
    @Test fun halfTurnIsDeliberateAndRetargetDoesNotJump() {
        val snap=OrientationSnap(); snap.sample(vector(180f),true,1000); snap.sample(vector(180f),true,1300)
        assertEquals(90f,abs(snap.value(1625)),.01f)
        assertEquals(180f,abs(snap.value(1950)),.01f)
        snap.sample(vector(90f),true,2000)
        val before=snap.value(2250); assertTrue(snap.sample(vector(90f),true,2250))
        assertEquals(0f,OrientationSnap.delta(before,snap.value(2250)),.01f)
        assertEquals(90f,snap.value(2700),.01f)
    }
    @Test fun rapidKeyboardChangesAndRotationKeepWaitersWithinBounds() {
        val w=World(); w.resize(400f,700f,72f,104f,0)
        w.keyboard(true,1000); w.tick(1000); w.tick(1500)
        w.keyboard(false,1600); w.keyboard(true,1650)
        w.resize(300f,160f,104f,72f,1700)
        for(t in 1700L..5000L step 32) w.tick(t)
        assertTrue(w.pets.all { it.state==State.PEEKING && it.x in 0f..196f && it.y in 0f..88f })
        assertNull(w.prop); assertNull(w.bubble)
    }
    private fun vector(degrees: Float): GravityVector {
        val r=Math.toRadians(degrees.toDouble()); return GravityVector(-sin(r).toFloat(),cos(r).toFloat())
    }
    @Test fun jitterAndBriefTiltsDoNotCommitTurns() {
        val snap=OrientationSnap()
        for(t in 0L..2000L step 100) assertFalse(snap.sample(vector(if(t%200==0L) 44f else 51f),true,t))
        assertEquals(0f,snap.target)
        snap.sample(vector(80f),true,2100); assertFalse(snap.sample(vector(80f),true,2300))
        snap.sample(vector(0f),true,2340); assertEquals(0f,snap.target)
    }
    @Test fun stableQuarterTurnEasesToExactTarget() {
        val snap=OrientationSnap(); snap.sample(vector(90f),true,1000)
        assertTrue(snap.sample(vector(90f),true,1250)); assertEquals(0f,snap.value(1250),.01f)
        assertEquals(45f,snap.value(1475),.01f); assertEquals(90f,snap.value(1700),.01f)
        assertFalse(snap.moving(1700))
        assertFalse(snap.sample(vector(43f),true,1800)); assertEquals(90f,snap.target)
    }
    @Test fun wrapAroundUsesShortestPathAndFlatPhoneHoldsOrientation() {
        assertEquals(20f,OrientationSnap.delta(170f,-170f),.01f)
        val snap=OrientationSnap(); snap.sample(vector(-90f),true,0); snap.sample(vector(-90f),true,300)
        assertEquals(-90f,snap.value(1000),.01f)
        snap.sample(vector(0f),false,1100); snap.sample(vector(0f),false,2000)
        assertEquals(-90f,snap.target)
    }
    @Test fun rotatedFootprintContainsBothAxes() {
        val quarter=OrientationSnap.extent(72f,104f,90f)
        assertEquals(104f,quarter.first,.01f); assertEquals(72f,quarter.second,.01f)
        val diagonal=OrientationSnap.extent(72f,104f,45f)
        assertTrue(diagonal.first>=124f && diagonal.second>=124f)
    }
    @Test fun keyboardInterruptsScenesThenWaitsQuietlyAndReturns() {
        val w=World(); w.resize(400f,700f,72f,104f,0)
        w.pets.forEach { it.y=596f; it.motion.grounded=true; it.state=State.IDLE }
        w.start(Kind.SNACK,Who.HUSBAND,1000)
        w.resize(400f,350f,72f,104f,1100); w.keyboard(true,1100)
        for(t in 1100L..5000L step 32) w.tick(t)
        assertTrue(w.pets.all { it.state==State.PEEKING && it.y<=246f })
        assertNull(w.prop); assertNull(w.interaction); assertFalse(w.start(Kind.KISS,Who.WIFE,6000))
        w.command(Command.Drag(Who.WIFE,100f,100f),6100); assertEquals(State.PEEKING,w.pet(Who.WIFE).state)
        w.tick(90000); assertNull(w.bubble); assertEquals(State.PEEKING,w.pet(Who.HUSBAND).state)
        w.resize(400f,700f,72f,104f,91000); w.keyboard(false,91000)
        for(t in 91000L..95000L step 32) w.tick(t)
        assertFalse(w.returningFromKeyboard); assertTrue(w.pets.all { it.x>0 && it.x<328f })
    }
    @Test fun keyboardSourcesHandleFloatingBoundsAndRevocation() {
        val s=KeyboardSignals(); s.insetBottom=300; assertEquals(300,s.bottom(1000))
        s.windowVisible=true; s.windowTop=430; assertEquals(570,s.bottom(1000))
        s.windowVisible=false; assertEquals(0,s.bottom(1000))
        s.windowVisible=null; assertEquals(300,s.bottom(1000))
        s.insetBottom=0; assertEquals(0,s.bottom(1000))
    }
}
