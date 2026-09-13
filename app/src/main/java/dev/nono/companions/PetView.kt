package dev.nono.companions

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Playback is independent of simulation. Clips enter, loop, and interrupt on state changes. */
data class Clip(val frames: IntArray, val durations: LongArray, val loop: Boolean) {
    val total=durations.sum()
}
class AnimationPlayer {
    private var pose: Pose?=null
    private var entered=0L
    var nextFrameAt=0L; private set
    private val clips=mapOf(
        Pose.PEEK to Clip(intArrayOf(42,43,42),longArrayOf(3500,140,1600),true),
        Pose.PARACHUTE to Clip(intArrayOf(40,41),longArrayOf(450,450),true),
        Pose.KISS_ENTER to Clip(intArrayOf(36,37),longArrayOf(300,350),false),
        Pose.IDLE to Clip(intArrayOf(4,5,6,5,7),longArrayOf(4200,80,110,80,1500),true),
        Pose.WALK to Clip(intArrayOf(0,1,2,3),longArrayOf(140,140,140,140),true),
        Pose.EAT to Clip(intArrayOf(24,25,26,27),longArrayOf(230,280,230,450),true),
        Pose.CLAW to Clip(intArrayOf(12,13,14,13),longArrayOf(220,220,350,220),true),
        Pose.REST to Clip(intArrayOf(28,29,30,31),longArrayOf(900,800,6500,1000),true),
        Pose.HUG to Clip(intArrayOf(16,17,18),longArrayOf(300,350,1800),false),
        Pose.REACH to Clip(intArrayOf(20,21,22),longArrayOf(200,200,600),false),
        Pose.ANTIC to Clip(intArrayOf(4,32,33,32),longArrayOf(350,240,400,240),true),
        Pose.FIX to Clip(intArrayOf(34,35),longArrayOf(320,320),true),
        Pose.FALL to Clip(intArrayOf(8,9),longArrayOf(220,220),true),
        Pose.LAND to Clip(intArrayOf(10,11),longArrayOf(200,350),false)
    )
    fun frame(next: Pose, now: Long): Int {
        if(next!=pose) { pose=next; entered=now }
        val clip=clips[next] ?: run { nextFrameAt=Long.MAX_VALUE; return next.frame }
        var elapsed=(now-entered).coerceAtLeast(0)
        if(clip.loop) elapsed %= clip.total
        else if(elapsed>=clip.total) { nextFrameAt=Long.MAX_VALUE; return clip.frames.last() }
        for(i in clip.frames.indices) {
            if(elapsed<clip.durations[i]) { nextFrameAt=now+clip.durations[i]-elapsed; return clip.frames[i] }
            elapsed-=clip.durations[i]
        }
        return clip.frames.last()
    }
}
class Art(context: Context) {
    val atlases = Array(2) { who -> Array(3) { sheet ->
        val character=if(who==0) "husband" else "wife"
        val kind=listOf("motion","social","personal")[sheet]
        context.assets.open("art/$character-$kind.png").use { BitmapFactory.decodeStream(it) }
    } }
    val bounds=Array(2) { who -> Array(3) { sheet -> Array(12) { frame ->
        val row=frame/4; val col=frame%4
        val bitmap=atlases[who][sheet]; val cw=bitmap.width/4; val ch=bitmap.height/3
        var left=cw; var top=ch; var right=0; var bottom=0
        for(y in 0 until ch) for(x in 0 until cw) if(Color.alpha(bitmap.getPixel(col*cw+x,row*ch+y)) > 40) { left=minOf(left,x); right=maxOf(right,x); top=minOf(top,y); bottom=maxOf(bottom,y) }
        if(right<=left) Rect(col*cw,row*ch,(col+1)*cw,(row+1)*ch) else Rect(col*cw+left,row*ch+top,col*cw+right+1,row*ch+bottom+1)
    } } }
    val peeking=context.assets.open("art/peeking.png").use { BitmapFactory.decodeStream(it) }
    val parachutes=context.assets.open("art/parachutes.png").use { BitmapFactory.decodeStream(it) }
    val kiss=context.assets.open("art/kiss.png").use { BitmapFactory.decodeStream(it) }
    val menus=context.assets.open("art/menus.png").use { BitmapFactory.decodeStream(it) }
    val bubbles=context.assets.open("art/bubbles.png").use { BitmapFactory.decodeStream(it) }
    val props=context.assets.open("art/props.png").use { BitmapFactory.decodeStream(it) }
}
class PetView(context: Context, private val who: Who, private val art: Art) : View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val player=AnimationPlayer()
    private val destination=RectF()
    private val kissSource=Rect()
    private val heartCell=Rect(art.props.width/3,art.props.height/2,art.props.width*2/3,art.props.height)
    private var bodyWidth=72f*resources.displayMetrics.density
    private var bodyHeight=104f*resources.displayMetrics.density
    private var angle=0f
    fun orientation(width: Float,height: Float,degrees: Float) {
        if(bodyWidth!=width || bodyHeight!=height || angle!=degrees) { bodyWidth=width; bodyHeight=height; angle=degrees; invalidate() }
    }
    private var frame=4
    private var facingLeft=false
    private var hearts=emptyList<KissHeart>()
    private var time=0L
    fun update(world: World, now: Long) {
        val hadHearts=hearts.isNotEmpty(); hearts=world.hearts.filter { it.who==who }; time=now
        if(hadHearts || hearts.isNotEmpty()) invalidate()
        val newFrame=player.frame(world.pose(who,now),now)
        val left=world.facesLeft(who)
        if(newFrame!=frame || left!=facingLeft) { frame=newFrame; facingLeft=left; invalidate() }
    }
    fun nextFrameDelay(now: Long)=(player.nextFrameAt-now).coerceIn(40,1000)
    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(width/2f,height/2f)
        canvas.rotate(angle)
        if(frame>=42) {
            val cw=art.peeking.width/2; val ch=art.peeking.height/2
            kissSource.set((frame-42)*cw,who.ordinal*ch,(frame-41)*cw,(who.ordinal+1)*ch)
            val w=38f*resources.displayMetrics.density; val h=44f*resources.displayMetrics.density
            if(who==Who.WIFE) canvas.scale(-1f,1f)
            destination.set(-w/2,-h/2,w/2,h/2)
            canvas.drawBitmap(art.peeking,kissSource,destination,paint)
            canvas.restore(); return
        }
        canvas.translate(-bodyWidth/2,-bodyHeight/2)
        val sheet=frame/12; val cell=frame%12
        val bitmap=if(frame>=40) art.parachutes else if(frame>=36) art.kiss else art.atlases[who.ordinal][sheet]
        if(frame>=40) kissSource.set((frame-39)*bitmap.width/3,who.ordinal*bitmap.height/2,(frame-38)*bitmap.width/3,(who.ordinal+1)*bitmap.height/2)
        else if(frame>=36) kissSource.set((frame-36)*bitmap.width/4,who.ordinal*bitmap.height/2,(frame-35)*bitmap.width/4,(who.ordinal+1)*bitmap.height/2)
        val source=if(frame>=36) kissSource else art.bounds[who.ordinal][sheet][cell]
        // Uniform scale uses original cell height, preserving seated/standing head scale.
        val scale=bodyHeight/(bitmap.height/(if(frame>=36) 2f else 3f)) * if(who==Who.WIFE) .9f else .97f
        val dw=source.width()*scale; val dh=source.height()*scale
        val bottom=bodyHeight-2
        canvas.save()
        if(facingLeft) canvas.scale(-1f,1f,bodyWidth/2f,bodyHeight/2f)
        paint.color=Color.WHITE
        destination.set((bodyWidth-dw)/2,bottom-dh,(bodyWidth+dw)/2,bottom)
        canvas.drawBitmap(bitmap,source,destination,paint)
        canvas.restore()
        for(heart in hearts) {
            val progress=heart.progress(time)
            val size=bodyWidth*.12f
            val x=heart.x*bodyWidth
            val y=heart.y*bodyHeight-progress*bodyHeight*.12f
            paint.alpha=((1-progress)*255).toInt()
            destination.set(x-size/2,y-size/2,x+size/2,y+size/2)
            canvas.drawBitmap(art.props,heartCell,destination,paint)
        }
        paint.alpha=255
        canvas.restore()
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

/** Separate, non-interactive prop surface; ownership does not alter character pixels. */
class PropView(context: Context, val type: PropType, private val atlas: Bitmap) : View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source=Rect()
    private val destination=RectF()
    private var angle=0f
    fun orientation(degrees: Float) { if(angle!=degrees) { angle=degrees; invalidate() } }
    private var bitten=false
    fun setBitten(value: Boolean) { if(bitten!=value) { bitten=value; invalidate() } }
    override fun onDraw(c: Canvas) {
        val frame=when(type) { PropType.COOKIE -> if(bitten) 1 else 0; PropType.TOOL -> 3; PropType.DARK_HEART -> 4 }
        val cw=atlas.width/3; val ch=atlas.height/2
        source.set((frame%3)*cw,(frame/3)*ch,(frame%3+1)*cw,(frame/3+1)*ch)
        val size=22f*resources.displayMetrics.density
        c.save(); c.translate(width/2f,height/2f); c.rotate(angle)
        destination.set(-size/2,-size/2,size/2,size/2)
        c.drawBitmap(atlas,source,destination,paint); c.restore()
    }
}
