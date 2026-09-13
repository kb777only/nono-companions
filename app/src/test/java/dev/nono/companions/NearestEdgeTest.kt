package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class NearestEdgeTest {
    private fun world(boyX: Float,girlX: Float)=World().apply {
        resize(400f,350f,72f,104f,0)
        pets[0].x=boyX; pets[1].x=girlX
        pets.forEach { it.y=240f }
        keyboard(true,1000)
    }
    @Test fun eitherCharacterCanChooseEitherEdge() {
        val w=world(290f,20f)
        assertFalse(w.pets[0].hideLeft); assertTrue(w.pets[1].hideLeft)
        for(t in 1000L..4000L step 32) w.tick(t)
        assertEquals(328f,w.pets[0].x,.1f); assertEquals(0f,w.pets[1].x,.1f)
    }
    @Test fun sharedEdgeStacksHerBelowHimOnBothSides() {
        for(left in listOf(true,false)) {
            val w=if(left) world(20f,70f) else world(260f,300f)
            assertEquals(left,w.pets[0].hideLeft); assertEquals(left,w.pets[1].hideLeft)
            val boy=w.peekTop(Who.HUSBAND,44f); val girl=w.peekTop(Who.WIFE,44f)
            assertTrue(girl>boy); assertEquals(37.4f,girl-boy,.01f)
            assertTrue(boy>=0 && girl+44<=350)
        }
    }
    @Test fun choiceStaysFixedUntilTheNextKeyboardOpening() {
        val w=world(20f,70f)
        w.pets[0].x=300f; w.keyboard(true,2000)
        assertTrue(w.pets[0].hideLeft)
        w.keyboard(false,2100); w.keyboard(true,2200)
        assertFalse(w.pets[0].hideLeft)
    }
    @Test fun stackedHeadsRemainOrderedAndBoundedAfterResize() {
        val w=world(20f,70f); w.resize(250f,65f,104f,72f,2000)
        val boy=w.peekTop(Who.HUSBAND,58f); val girl=w.peekTop(Who.WIFE,58f)
        assertTrue(girl>boy); assertTrue(boy>=0 && girl+58<=65)
    }
    @Test fun distanceUsesCharacterCenterWithStableTieBreak() {
        val w=world(164f,165f)
        assertTrue(w.pets[0].hideLeft); assertFalse(w.pets[1].hideLeft)
    }
}
