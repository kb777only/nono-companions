package dev.nono.companions

import java.io.Reader
import kotlin.math.*

data class SpriteMeasure(val asset: String,val index: Int,val x: Int,val y: Int,val width: Int,val height: Int,
    val faceX: Float,val faceY: Float,val faceSpan: Float,val referenceSpan: Float) {
    fun placement(unit: Float=1f,peek: Boolean=false): SpritePlacement {
        val scale=referenceSpan/faceSpan*unit
        val w=width*scale; val h=height*scale
        return SpritePlacement(-w/2,if(peek) -h/2 else 50f*unit-h,w,h,scale,(-w/2+faceX*scale),if(peek) -h/2+faceY*scale else 50f*unit-h+faceY*scale)
    }
}
data class SpritePlacement(val left: Float,val top: Float,val width: Float,val height: Float,val scale: Float,val faceX: Float,val faceY: Float) {
    fun bounds(angle: Float,leftFacing: Boolean=false): FloatArray {
        val r=angle*PI/180; val c=cos(r).toFloat(); val s=sin(r).toFloat()
        val xs=FloatArray(4); val ys=FloatArray(4)
        for(i in 0..3) { val x=(left+if(i%2==0) 0f else width)*(if(leftFacing) -1 else 1); val y=top+if(i<2) 0f else height; xs[i]=x*c-y*s; ys[i]=x*s+y*c }
        return floatArrayOf(xs.min(),ys.min(),xs.max(),ys.max())
    }
}

/** The only character source selector and scale calculation, shared by every pose. */
class CharacterGeometry(reader: Reader) {
    val measures: Map<String,SpriteMeasure> = reader.buffered().useLines { lines -> lines.drop(1).filter { it.isNotBlank() }.map { line ->
        val p=line.split(','); val m=SpriteMeasure(p[0],p[1].toInt(),p[2].toInt(),p[3].toInt(),p[4].toInt(),p[5].toInt(),p[6].toFloat(),p[7].toFloat(),p[8].toFloat(),p[9].toFloat())
        require(m.width>0 && m.height>0 && m.faceSpan>0 && m.referenceSpan>0)
        "${m.asset}:${m.index}" to m
    }.toMap() }
    fun select(who: Who,frame: Int,outfit: Outfit,rain: Boolean): SpriteMeasure {
        require(frame in 0..63)
        val pair=when {
            frame in 42..43 -> "peeking" to who.ordinal*2+frame-42
            frame !in 40..47 && umbrellaPose(rain,frame) -> "w10-${who.name.lowercase()}-rain-${outfit.name.lowercase()}" to frame
            frame !in 40..47 && outfit!=Outfit.DEFAULT -> "w10-${who.name.lowercase()}-${outfit.name.lowercase()}" to frame
            umbrellaPose(rain,frame) -> "rain-${outfit.name.lowercase()}" to who.ordinal*12+rainFrame(frame)
            frame in 44..47 -> "cooling" to who.ordinal*4+frame-44
            outfit!=Outfit.DEFAULT -> "weather-${outfit.name.lowercase()}" to who.ordinal*12+dressedFrame(frame)
            frame>=48 -> "${who.name.lowercase()}-idle" to frame-48
            frame>=40 -> "parachutes" to who.ordinal*3+frame-39
            frame>=36 -> "kiss" to who.ordinal*4+frame-36
            else -> "${who.name.lowercase()}-${listOf("motion","social","personal")[frame/12]}" to frame%12
        }
        return measures.getValue("${pair.first}:${pair.second}")
    }
    companion object {
        const val OVERFLOW_WIDTH=160f
        const val OVERFLOW_HEIGHT=240f
        /** Translate, never rescale, at screen edges. */
        fun visibleCenter(center: Float,min: Float,max: Float,extent: Float): Float {
            val low=-min; val high=extent-max
            return if(low<=high) center.coerceIn(low,high) else extent/2-(min+max)/2
        }
    }
}
