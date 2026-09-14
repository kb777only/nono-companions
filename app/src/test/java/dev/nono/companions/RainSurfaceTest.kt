package dev.nono.companions

import org.junit.Assert.*
import org.junit.Test

class RainSurfaceTest {
    @Test fun measuredContourFollowsPlacementAndFacing() {
        val s=RainSurface(-.1f,-.2f,1.2f,1.5f,floatArrayOf(.2f,.04f,.12f))
        assertEquals(-.14f,s.at(.5f)!!,.00001f)
        assertEquals(.1f,s.at(-.1f)!!,.00001f)
        assertEquals(s.at(.2f)!!,s.copy(mirrored=true).at(.8f)!!,.00001f)
        assertNull(s.at(1.2f))
    }
    @Test fun gapsNeverFallBackToAnInvisibleHeadOrCanopyPlane() {
        val p=RainParticles();p.canopySurface=RainSurface(0f,.4f,1f,1f,floatArrayOf(Float.NaN,.1f,.2f,Float.NaN))
        assertNull(p.surface(.05f,true))
        assertEquals(.55f,p.surface(.5f,true)!!,.00001f)
    }
    @Test fun dropBouncesAtMeasuredPixelsRatherThanOldEstimatedHeight() {
        val p=RainParticles();p.canopySurface=RainSurface(0f,.6f,1f,1f,floatArrayOf(0f,0f))
        val d=WaterDrop(.5f,.5f,0f,2f,1000,2000);p.drops.add(d)
        p.tick(1000,true,false);p.tick(1100,true,false)
        assertTrue(d.bounced);assertEquals(.6f,d.y,.00001f)
    }
}
