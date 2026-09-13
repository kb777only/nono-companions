package dev.nono.companions

import android.content.Context
import android.graphics.*
import android.view.View

/** A separate pass-through canopy; cords meet the raised hands without enlarging pet touch targets. */
class ParachuteView(context: Context, atlas: Bitmap, who: Who): View(context) {
    private val bitmap=atlas
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source=Rect(0,who.ordinal*atlas.height/2,atlas.width/3,who.ordinal*atlas.height/2+atlas.height/4)
    private val destination=RectF()
    private var angle=0f
    fun orientation(degrees: Float) { if(angle!=degrees) { angle=degrees; invalidate() } }
    override fun onDraw(canvas: Canvas) {
        val w=96f*resources.displayMetrics.density; val h=90f*resources.displayMetrics.density
        canvas.save(); canvas.translate(width/2f,height/2f); canvas.rotate(angle); canvas.translate(-w/2,-h/2)
        val canopyHeight=h*.52f
        paint.color=Color.rgb(224,220,204); paint.strokeWidth=resources.displayMetrics.density*.8f
        for(i in 0..3) {
            val start=w*(.10f+i*.265f)
            val end=w*(if(i<2) .31f else .69f)
            canvas.drawLine(start,canopyHeight*.70f,end,h,paint)
        }
        paint.color=Color.WHITE
        destination.set(0f,0f,w,canopyHeight)
        canvas.drawBitmap(bitmap,source,destination,paint)
        canvas.restore()
    }
}
