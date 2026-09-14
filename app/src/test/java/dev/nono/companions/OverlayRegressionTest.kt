package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class OverlayRegressionTest {
    @Test fun canopyExpiresWithoutFurtherSimulationFrames() {
        val lease=ArtLease(); assertFalse(lease.visible(0))
        lease.renew(1000); assertTrue(lease.visible(1499)); assertFalse(lease.visible(1500))
        lease.renew(1600); lease.clear(); assertFalse(lease.visible(1601))
    }
    @Test fun edgeContactClosesCanopyEvenWhenSlidingTooFastToBeSettled() {
        val physics=GravityPhysics(); physics.setGravity(.7f,.7f)
        val p=Pet(Who.HUSBAND,100f,596f,state=State.PARACHUTING,motion=Motion(vx=160f,fallSince=1000))
        assertFalse(canopyVisible(p,physics,400f,700f,72f,104f))
        physics.step(p,400f,700f,72f,104f,.016f,2000)
        assertFalse(p.motion.grounded); assertEquals(State.RECOVERING,p.state)
        assertEquals(-1L,p.motion.fallSince)
    }
    @Test fun stateAloneCannotKeepAnOldCanopyAlive() {
        val physics=GravityPhysics(); val p=Pet(Who.WIFE,30f,30f,state=State.PARACHUTING)
        assertFalse(canopyVisible(p,physics,400f,700f,72f,104f))
        p.motion.fallSince=1000; assertTrue(canopyVisible(p,physics,400f,700f,72f,104f))
        p.state=State.DRAGGED; assertFalse(canopyVisible(p,physics,400f,700f,72f,104f))
    }
    @Test fun aNegativeWindowSnapshotCannotMaskPositiveImeInsets() {
        val k=KeyboardSignals(); k.windowVisible=false; k.insetBottom=320
        assertTrue(k.visible); assertEquals(320,k.bottom(1000))
        k.insetBottom=0; assertFalse(k.visible); assertEquals(0,k.bottom(1000))
        k.windowVisible=true; k.windowTop=650
        assertTrue(k.visible); assertEquals(350,k.bottom(1000))
        k.windowVisible=null; assertFalse(k.visible)
    }
    @Test fun integratedRainPosesCoverActionsButYieldToFlightAndKeyboard() {
        for(frame in 0..47) assertTrue(rainFrame(frame) in 0..11)
        for(frame in listOf(0,4,12,20,24,28,34,38,44,47)) assertTrue(umbrellaPose(true,frame))
        for(frame in listOf(8,9,10,11,40,41,42,43)) assertFalse(umbrellaPose(true,frame))
        assertFalse(umbrellaPose(false,4))
    }
}
