package dev.nono.companions

import android.content.Context
import android.graphics.*
import android.view.View

/** Droplets only. The umbrella is painted into each character rain pose. */
class RainView(context: Context): View(context) {
    val particles=RainParticles()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var angle=0f; private var time=0L; private var umbrella=true
    private var lastX: Float?=null; private var lastY=0f
    fun update(p: Pet,now: Long,degrees: Float,holdsUmbrella: Boolean,surface: Pair<Float,Float>) {
        angle=degrees; time=now
        umbrella=holdsUmbrella
        particles.canopyTop=surface.first; particles.canopyRadius=surface.second
        val density=resources.displayMetrics.density
        val rawX=p.x-(lastX ?: p.x); val rawY=if(lastX==null) 0f else p.y-lastY
        val r=Math.toRadians(-degrees.toDouble())
        val dx=(rawX*kotlin.math.cos(r)-rawY*kotlin.math.sin(r)).toFloat()/(72*density)
        val dy=(rawX*kotlin.math.sin(r)+rawY*kotlin.math.cos(r)).toFloat()/(104*density)
        particles.tick(now,umbrella,p.state in listOf(State.WANDERING,State.APPROACHING),dx,dy)
        lastX=p.x; lastY=p.y; invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        val density=resources.displayMetrics.density; val bw=72*density; val bh=104*density
        canvas.save(); canvas.translate(width/2f,height/2f); canvas.rotate(angle); canvas.translate(-bw/2,-bh/2)
        paint.color=Color.rgb(84,178,241); paint.strokeCap=Paint.Cap.ROUND
        for(drop in particles.drops) {
            paint.alpha=(drop.alpha(time)*255).toInt(); paint.strokeWidth=density*1.5f
            val x=drop.x*bw; val y=drop.y*bh
            if(drop.trail || drop.bounced) canvas.drawCircle(x,y,density*1.4f,paint)
            else canvas.drawLine(x,y,x-drop.vx*bw*.025f,y-drop.vy*bh*.025f,paint)
        }
        paint.alpha=255; canvas.restore()
    }
}
