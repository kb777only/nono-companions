package dev.nono.companions

import java.io.File
import java.io.DataInputStream
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class CharacterGeometryTest {
    private val art=File("src/main/assets/art")
    private fun geometry()=CharacterGeometry(File(art,"character-measures.csv").reader())
    @Test fun everyRouteUsesCalibratedCompleteAssetAndOneUniformScale() {
        val g=geometry(); val visited=mutableSetOf<String>()
        val sizes=g.measures.values.map { it.asset }.distinct().associateWith { name -> DataInputStream(File(art,"calibrated/$name.png").inputStream()).use { it.skipBytes(16); it.readInt() to it.readInt() } }
        for(who in Who.entries) for(outfit in Outfit.entries) for(rain in listOf(false,true)) for(frame in 0..63) {
            val m=g.select(who,frame,outfit,rain); visited.add("${m.asset}:${m.index}")
            val size=sizes.getValue(m.asset)
            assertTrue(m.x>=0 && m.y>=0 && m.x+m.width<=size.first && m.y+m.height<=size.second)
            for(density in listOf(.75f,1f,1.5f,2.75f,3f,4f)) {
                val p=m.placement(density,frame in 42..43)
                assertEquals(m.referenceSpan*density,m.faceSpan*p.scale,.0001f)
                assertEquals(p.width/m.width,p.height/m.height,.00001f)
                assertTrue(p.left>=-80*density && p.left+p.width<=80*density)
                assertTrue("${m.asset}:${m.index} top ${p.top}",p.top>=-120*density && p.top+p.height<=120*density)
            }
        }
        assertEquals(910,visited.size); assertEquals(g.measures.keys,visited)
    }
    @Test fun everyIntermediateRotationPreservesScaleAndFitsTheDrawingSurface() {
        val g=geometry()
        for(m in g.measures.values) for(angle in -180..180 step 5) for(mirror in listOf(false,true)) {
            val p=m.placement(peek=m.asset=="peeking"); val b=p.bounds(angle.toFloat(),mirror)
            val extent=OrientationSnap.extent(160f,240f,angle.toFloat())
            assertTrue("${m.asset}:${m.index} at $angle",b[0]>=-extent.first/2-.001 && b[2]<=extent.first/2+.001 && b[1]>=-extent.second/2-.001 && b[3]<=extent.second/2+.001)
        }
    }
    @Test fun edgesTranslateWholeArtworkWithoutChangingItsSize() {
        for(m in geometry().measures.values) for(angle in listOf(-180f,-90f,-35f,0f,45f,90f,180f)) {
            val b=m.placement(peek=m.asset=="peeking").bounds(angle)
            for(width in listOf(240f,360f,740f)) for(height in listOf(240f,600f,900f)) for(edge in listOf(0f,1f)) {
                val x=CharacterGeometry.visibleCenter(width*edge,b[0],b[2],width)
                val y=CharacterGeometry.visibleCenter(height*edge,b[1],b[3],height)
                assertTrue(x+b[0]>=-.001f && x+b[2]<=width+.001f)
                assertTrue(y+b[1]>=-.001f && y+b[3]<=height+.001f)
            }
        }
    }
    @Test fun rainAndPeekCannotUseAnIndependentFitToWindowScale() {
        val g=geometry()
        for(who in Who.entries) {
            val reference=g.select(who,4,Outfit.DEFAULT,false).referenceSpan
            for(m in g.measures.values.filter { it.referenceSpan==reference }) {
                assertEquals(reference,m.placement(peek=m.asset=="peeking").scale*m.faceSpan,.00001f)
            }
        }
    }
    @Test fun seasonalAndRainClipsHaveDistinctFrameAssetsInsteadOfCollapsedFallbacks() {
        val g=geometry()
        for(who in Who.entries) for(outfit in Outfit.entries) for(rain in listOf(false,true)) {
            for(sequence in listOf(0..3,4..7,12..14,16..18,20..22,24..27,28..31,32..35,36..39,48..51,52..55,56..59,60..63)) {
                val selected=sequence.map { g.select(who,it,outfit,rain) }
                assertEquals(sequence.count(),selected.map { "${it.asset}:${it.index}" }.toSet().size)
                if(rain || outfit!=Outfit.DEFAULT) assertTrue(selected.all { it.asset.startsWith("w11-") })
            }
        }
    }
    @Test fun changingWeatherKeepsNeutralFaceAtOriginalHeightAboveFeet() {
        val g=geometry()
        for(who in Who.entries) {
            val original=g.select(who,4,Outfit.DEFAULT,false).placement()
            for(outfit in Outfit.entries) for(rain in listOf(false,true)) {
                if(outfit==Outfit.DEFAULT && !rain) continue
                for(frame in listOf(4,15,26,52)) {
                    val pose=g.select(who,frame,outfit,rain).placement()
                    assertEquals("$who $outfit rain=$rain frame=$frame",original.faceY,pose.faceY,.02f)
                }
            }
        }
    }
}
