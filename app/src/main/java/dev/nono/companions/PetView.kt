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
        Pose.FEET to Clip(intArrayOf(48,49,50,49,50,51),longArrayOf(550,280,280,280,280,800),true),
        Pose.STRETCH to Clip(intArrayOf(52,53,54,53,55,52),longArrayOf(400,750,850,400,850,500),false),
        Pose.SIGNATURE to Clip(intArrayOf(56,57,58,59),longArrayOf(650,650,1000,1400),false),
        Pose.RUN to Clip(intArrayOf(60,61,62,63),longArrayOf(150,150,150,150),true),
        Pose.FAN to Clip(intArrayOf(44,45),longArrayOf(320,320),true),
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
    val geometry=context.assets.open("art/character-measures.csv").reader().use { CharacterGeometry(it) }
    val characterBitmaps=geometry.measures.values.map { it.asset }.distinct().associateWith { name ->
        context.assets.open("art/calibrated/$name.png").use { BitmapFactory.decodeStream(it) }
    }
    val coolingProps=context.assets.open("art/cooling-props.png").use { BitmapFactory.decodeStream(it) }
    val parachutes=context.assets.open("art/parachutes.png").use { BitmapFactory.decodeStream(it) }
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
        if(bodyWidth!=width || bodyHeight!=height || angle!=degrees) { bodyWidth=width; bodyHeight=height; angle=degrees; changed() }
    }
    private var hot=false
    private var coolingPhase=CoolingPhase.NONE
    private var coolingElapsed=0L
    private val coolingArt=CoolingArt(art.coolingProps)
    private var raining=false
    private var outfit=Outfit.DEFAULT
    private var frame=4
    private var facingLeft=false
    private var hearts=emptyList<KissHeart>()
    private var time=0L
    fun update(world: World, now: Long) {
        if(hot || world.deviceHeat.hot) changed()
        hot=world.deviceHeat.hot
        coolingPhase=world.cooling[who.ordinal].phase
        coolingElapsed=world.cooling[who.ordinal].elapsed(now)
        val hadHearts=hearts.isNotEmpty(); hearts=world.hearts.filter { it.who==who }; time=now
        if(hadHearts || hearts.isNotEmpty()) changed()
        if(raining!=world.weather.raining) { raining=world.weather.raining; changed() }
        if(outfit!=world.weather.outfit) { outfit=world.weather.outfit; changed() }
        val newFrame=player.frame(world.pose(who,now),now)
        val left=if(newFrame in 42..43) !world.pet(who).hideLeft else world.facesLeft(who)
        if(newFrame!=frame || left!=facingLeft) { frame=newFrame; facingLeft=left; changed() }
    }
    fun nextFrameDelay(now: Long)=(player.nextFrameAt-now).coerceIn(40,1000)
    val overflow = object: View(context) {
        override fun onDraw(canvas: Canvas) { drawLayer(canvas,width,height,true) }
    }
    private fun changed() { invalidate(); overflow.invalidate() }
    private fun measure()=art.geometry.select(who,frame,outfit,raining)
    private fun placement()=measure().placement(bodyHeight/104f,frame in 42..43)
    fun rainSurface(): Pair<Float,Float> { val p=placement(); return (p.top+bodyHeight/2)/bodyHeight to p.width/bodyWidth/2 }
    fun contentBounds(degrees: Float)=placement().bounds(degrees,facingLeft)
    fun facePosition(): Pair<Float,Float> { val p=placement(); return (if(facingLeft) -p.faceX else p.faceX) to p.faceY }
    override fun onDraw(canvas: Canvas) { drawLayer(canvas,width,height,false) }
    private fun drawLayer(canvas: Canvas,viewportWidth: Int,viewportHeight: Int,outside: Boolean) {
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
        canvas.save(); canvas.translate(viewportWidth/2f,viewportHeight/2f)
        if(outside) {
            // Exclude precisely the screen-aligned touch window; no seam overlaps.
            val core=OrientationSnap.extent(if(frame in 42..43) bodyHeight*72/104 else bodyWidth,if(frame in 42..43) bodyHeight*72/104 else bodyHeight,angle)
            canvas.clipOutRect(-kotlin.math.ceil(core.first/2),-kotlin.math.ceil(core.second/2),kotlin.math.ceil(core.first/2),kotlin.math.ceil(core.second/2))
        }
        canvas.rotate(angle)
        val m=measure(); val p=placement(); val bitmap=art.characterBitmaps.getValue(m.asset)
        kissSource.set(m.x,m.y,m.x+m.width,m.y+m.height)
        destination.set(p.left,p.top,p.left+p.width,p.top+p.height)
        canvas.save(); if(facingLeft) canvas.scale(-1f,1f)
        canvas.drawBitmap(bitmap,kissSource,destination,paint); canvas.restore()
        if(frame in 42..43) { canvas.restore(); return }
        canvas.translate(-bodyWidth/2,-bodyHeight/2)
        val rainPose=umbrellaPose(raining,frame)
        coolingArt.draw(canvas,who,coolingPhase,time,bodyWidth,bodyHeight,facingLeft,hot,frame==45,coolingElapsed,rainPose,bodyWidth/2+facePosition().first,bodyHeight/2+facePosition().second)
        for(heart in hearts) {
            val progress=heart.progress(time)
            val size=bodyWidth*.12f
            val face=facePosition()
            val x=bodyWidth/2+face.first+(heart.x-.5f)*bodyWidth*.7f
            val y=bodyHeight/2+face.second+(heart.y-.24f)*bodyHeight-progress*bodyHeight*.12f
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
