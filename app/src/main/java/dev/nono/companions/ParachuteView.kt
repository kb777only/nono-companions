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
    override fun onDraw(canvas: Canvas) {
        val canopyHeight=height*.52f
        paint.color=Color.rgb(224,220,204); paint.strokeWidth=resources.displayMetrics.density*.8f
        for(i in 0..3) {
            val start=width*(.10f+i*.265f)
            val end=width*(if(i<2) .31f else .69f)
            canvas.drawLine(start,canopyHeight*.70f,end,height.toFloat(),paint)
        }
        paint.color=Color.WHITE
        destination.set(0f,0f,width.toFloat(),canopyHeight)
        canvas.drawBitmap(bitmap,source,destination,paint)
    }
}
