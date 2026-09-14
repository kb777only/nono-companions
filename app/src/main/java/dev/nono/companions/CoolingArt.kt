package dev.nono.companions

import android.graphics.*
import kotlin.math.sin

/** Separate prop layers inside the existing small character window: no extra touch surface. */
class CoolingArt(private val atlas: Bitmap) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source=Rect()
    private val target=RectF()
    private val drop=Path()
    private fun cell(canvas: Canvas,who: Who,row: Int,x: Float,y: Float,w: Float,h: Float) {
        val cw=atlas.width/2; val ch=atlas.height/3
        source.set(who.ordinal*cw,row*ch,(who.ordinal+1)*cw,(row+1)*ch)
        target.set(x,y,x+w,y+h)
        paint.color=Color.WHITE; paint.alpha=255
        canvas.drawBitmap(atlas,source,target,paint)
    }
    fun draw(c: Canvas,who: Who,phase: CoolingPhase,time: Long,w: Float,h: Float,left: Boolean,hot: Boolean,raised: Boolean,elapsed: Long,rain: Boolean=false,faceX: Float=w/2,faceY: Float=h*.25f) {
        c.save()
        if(left) c.scale(-1f,1f,w/2,h/2)
        if(phase==CoolingPhase.FANNING) {
            c.save()
            val x=w*(if(rain) .76f else if(raised) .22f else if(who==Who.HUSBAND) .36f else .29f); val y=h*(if(rain) .64f else if(raised) .43f else .47f)
            c.rotate(sin(time/180.0).toFloat()*18f,x,y)
            cell(c,who,0,x-w*.21f,y-w*.28f,w*.42f,w*.42f)
            c.restore()
        }
        if(phase in listOf(CoolingPhase.PLACING,CoolingPhase.SWITCHING,CoolingPhase.BREEZE)) {
            // Body remains planted; the independently drawn rotor alone turns after switching on.
            val size=w*.43f; val x=w*.48f; val y=h-size-2-if(phase==CoolingPhase.PLACING) h*.12f*(1-(elapsed/900f).coerceIn(0f,1f)) else 0f
            val cx=x+size*.5f; val cy=y+size*.355f
            c.save(); c.rotate(if(phase==CoolingPhase.BREEZE) (time%1200)/1200f*360f else 0f,cx,cy)
            val rotorSize=size*.82f
            cell(c,who,2,cx-rotorSize/2,cy-rotorSize/2,rotorSize,rotorSize)
            c.restore()
            cell(c,who,1,x,y,size,size)
            if(phase==CoolingPhase.BREEZE) {
                paint.color=Color.rgb(164,231,183); paint.alpha=220
                c.drawCircle(x+size*.5f,y+size*.90f,size*.035f,paint)
            }
        }
        c.restore()
        if(hot) {
            // Bounded local face droplets, staggered fades and downward motion, no allocations per drop.
            for(i in 0..2) {
                val t=((time+i*530)%1900)/1900f
                val x=faceX+w*(if(i%2==0) -.18f else .18f)
                val y=faceY+h*(-.03f+i*.035f+t*.10f)
                val r=w*.026f
                paint.color=Color.rgb(100,195,242)
                paint.alpha=(sin(t*Math.PI).coerceAtLeast(0.0)*225).toInt()
                drop.reset(); drop.moveTo(x,y-r*2)
                drop.cubicTo(x-r*2,y+r,x+r*2,y+r,x,y-r*2)
                c.drawPath(drop,paint)
            }
            paint.alpha=255
        }
    }
}
