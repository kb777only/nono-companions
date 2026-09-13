package dev.nono.companions

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout

/** Only this small visible menu window receives touches; the surrounding screen stays usable. */
class ActionMenu(context: Context, private val art: Bitmap, private val who: Who, private val outside: ()->Unit, action: (Int)->Unit): FrameLayout(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source=Rect(who.ordinal*art.width/2,0,(who.ordinal+1)*art.width/2,art.height)
    private val destination=RectF()
    private val labels=listOf(if(who==Who.HUSBAND) "Goûter" else "Chiper", "Bisou", "Rawrr", "Dodo")
    init {
        setWillNotDraw(false)
        labels.forEachIndexed { index,label ->
            addView(Button(context).apply {
                text=label; contentDescription=label; isAllCaps=false; textSize=12f
                typeface=Typeface.create("casual",Typeface.BOLD)
                setTextColor(if(who==Who.HUSBAND) Color.rgb(47,58,34) else Color.rgb(63,30,62))
                setBackgroundColor(Color.TRANSPARENT); setPadding(0,0,0,0)
                gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setOnClickListener { action(index) }
            })
        }
    }
    override fun onLayout(changed: Boolean,left: Int,top: Int,right: Int,bottom: Int) {
        for(i in 0..3) {
            val x=(if(i%2==0) .16f else .51f)*width
            val y=(if(i<2) .14f else .50f)*height
            getChildAt(i).layout(x.toInt(),y.toInt(),(x+width*.34f).toInt(),(y+height*.31f).toInt())
        }
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked==MotionEvent.ACTION_OUTSIDE) { outside(); return true }
        return super.dispatchTouchEvent(event)
    }
    override fun onDraw(canvas: Canvas) {
        destination.set(0f,0f,width.toFloat(),height.toFloat())
        canvas.drawBitmap(art,source,destination,paint)
    }
}
