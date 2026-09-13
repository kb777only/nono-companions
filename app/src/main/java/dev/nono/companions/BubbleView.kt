package dev.nono.companions

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View

/** Generated skin and native text stay separate, so French stays crisp and accessible. */
class BubbleView(context: Context, bitmap: Bitmap, who: Who, private val words: String) : View(context) {
    private val skin=bitmap
    private val source=Rect(0,who.ordinal*bitmap.height/2,bitmap.width,(who.ordinal+1)*bitmap.height/2)
    private val destination=RectF()
    private val bitmapPaint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color=if(who==Who.HUSBAND) Color.rgb(53,37,24) else Color.rgb(63,31,65)
        textSize=12.5f*resources.displayMetrics.scaledDensity
        typeface=Typeface.create("casual",Typeface.BOLD)
    }
    private var textLayout: StaticLayout?=null
    private fun dp(v: Float)=v*resources.displayMetrics.density
    init { contentDescription=words; importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) {
        destination.set(0f,0f,w.toFloat(),h.toFloat())
        textLayout=StaticLayout.Builder.obtain(words,0,words.length,textPaint,(w-dp(38f)).toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(2).build()
    }
    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(skin,source,destination,bitmapPaint)
        textLayout?.let { text -> canvas.save(); canvas.translate(dp(16f),((height-dp(16f)-text.height)/2f).coerceAtLeast(dp(8f))); text.draw(canvas); canvas.restore() }
    }
}
