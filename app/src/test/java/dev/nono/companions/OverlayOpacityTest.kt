package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class OverlayOpacityTest {
    @Test fun simultaneousEffectsAndKeyboardHeadsStayBelowSystemLimit() {
        for(count in 0..12) {
            val requested=List(count) { listOf(.65f,.55f,.3f,.85f,.75f)[it%5] }
            val actual=OverlayOpacity.distribute(requested)
            assertTrue(1-actual.fold(1.0) { product,a -> product*(1-a) }<=.780001)
            actual.zip(requested).forEach { (a,r) -> assertTrue(a>0 && a<=r+.00001f) }
        }
    }
}
