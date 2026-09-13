package dev.nono.companions

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView

class CompanionService : Service(), DisplayManager.DisplayListener {
    companion object { private var reference=java.lang.ref.WeakReference<CompanionService>(null); val live: CompanionService? get()=reference.get() }
    private val handler=Handler(Looper.getMainLooper())
    private val world=World()
    private lateinit var wm: WindowManager
    private lateinit var store: MoodStore
    private lateinit var displays: DisplayManager
    private lateinit var tilt: TiltSensor
    private val views=mutableMapOf<Who,PetView>()
    private val params=mutableMapOf<Who,WindowManager.LayoutParams>()
    private var menu: ActionMenu?=null
    private var menuParams: WindowManager.LayoutParams?=null
    private var menuWho: Who?=null
    private val dismissMenu=Runnable { removeMenu() }
    private var gestureEpoch=0
    private val canopies=mutableMapOf<Who,Pair<ParachuteView,WindowManager.LayoutParams>>()
    private var speech: BubbleView?=null
    private lateinit var artwork: Art
    private var speechParams: WindowManager.LayoutParams?=null
    private var spoken: Bubble?=null
    private var propView: PropView?=null
    private var propParams: WindowManager.LayoutParams?=null
    private var propOwner: Who?=null
    private var propFromX=0f; private var propFromY=0f; private var propMovedAt=0L
    private var screenOn=true
    private var originX=0; private var originY=0
    private var geometry=""
    private var imeBottom=0
    private var savedAt=0L
    private var started=false
    private val receiver=object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { screenOn=false; tilt.stop(); handler.removeCallbacks(tick); world.held=null; gestureEpoch++; world.interrupt(now(),resetMotion=true); store.save(world); removeCanopies(); removeMenu(); removeSpeech(); removeProp(); views.values.forEach { it.visibility=View.GONE } }
                Intent.ACTION_SCREEN_ON,Intent.ACTION_USER_PRESENT -> { screenOn=true; tilt.start(); world.resetClock(); handler.removeCallbacks(tick); handler.post(tick) }
            }
        }
    }
    private fun now()=SystemClock.elapsedRealtime()
    private fun dp(v: Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate() {
        super.onCreate(); reference=java.lang.ref.WeakReference(this)
        wm=getSystemService(WindowManager::class.java); displays=getSystemService(DisplayManager::class.java); store=MoodStore(this); store.load(world)
        tilt=TiltSensor(this,{ displays.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0 }) { vector ->
            world.gravity(vector.x,vector.y,now())
            if(screenOn && world.pets.any { it.state!=State.DRAGGED && !world.physics.supported(it,world.width,world.height,world.petWidth,world.petHeight) }) {
                handler.removeCallbacks(tick); handler.post(tick)
            }
        }
        val channel=NotificationChannel("companions","Companion presence",NotificationManager.IMPORTANCE_LOW).apply { description="Required notification while companions are present"; setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"companions").setSmallIcon(android.R.drawable.btn_star_big_on).setContentTitle("Vos compagnons sont là").setContentText("De l’amour et des goûters volés · tout reste ici").setContentIntent(pending).setOngoing(true).build()
        if(Build.VERSION.SDK_INT >= 34) startForeground(1,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1,notification)
        if(!Settings.canDrawOverlays(this)) { stopSelf(); return }
        try {
            val art=Art(this); artwork=art
            Who.entries.forEach { who ->
                val view=PetView(this,who,art); val lp=layout(dp(72),dp(104),false)
                views[who]=view; params[who]=lp
                touch(view,who)
                view.setOnApplyWindowInsetsListener { _, insets -> val next=insets.getInsets(WindowInsets.Type.ime()).bottom; if(next!=imeBottom) { imeBottom=next; handler.post { bounds() } }; insets }
                wm.addView(view,lp)
            }
            bounds()
            world.pets.forEachIndexed { index,p -> p.x=world.width*(if(index==0) .16f else .60f); p.y=(world.height-world.petHeight-dp(45)).coerceAtLeast(0f) }
            bounds(force=true)
        } catch(_: Exception) { stopSelf(); return }
        val filter=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) }
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(receiver,filter)
        displays.registerDisplayListener(this,handler); started=true
        screenOn=getSystemService(PowerManager::class.java).isInteractive
        if(screenOn) { tilt.start(); handler.post(tick) }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    private fun layout(w: Int,h: Int,passThrough: Boolean)=WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if(passThrough) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0),PixelFormat.TRANSLUCENT).apply {
        gravity=Gravity.TOP or Gravity.LEFT
        // Bubbles remain below Android's maximum obscuring opacity for pass-through touches.
        if(passThrough) alpha=.75f
        softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        // Coordinates are absolute; bounds() applies cutout/system insets exactly once.
        layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setFitInsetsTypes(0)
    }
    private val tick=object: Runnable {
        override fun run() {
            if(!screenOn) return
            if(!Settings.canDrawOverlays(this@CompanionService)) { store.save(world); stopSelf(); return }
            val locked=getSystemService(KeyguardManager::class.java).isKeyguardLocked
            if(locked) { world.held=null; gestureEpoch++; removeCanopies(); removeMenu(); tilt.stop(); views.values.forEach { it.visibility=View.GONE }; removeSpeech(); removeProp(); handler.postDelayed(this,2000); return }
            tilt.start()
            views.values.forEach { it.visibility=View.VISIBLE }
            val time=now(); world.tick(time)
            try {
                world.pets.forEach { p -> val lp=params.getValue(p.who); val x=originX+p.x.toInt(); val y=originY+p.y.toInt()
                    if(lp.x!=x || lp.y!=y) { lp.x=x; lp.y=y; wm.updateViewLayout(views.getValue(p.who),lp) }
                    views.getValue(p.who).update(world,time)
                }
                if(menu==null) showSpeech() else { removeSpeech(); positionMenu(); menu?.let { wm.updateViewLayout(it,menuParams) } }
                showProp(time)
                showCanopies()
            } catch(_: WindowManager.BadTokenException) { stopSelf(); return } catch(_: SecurityException) { stopSelf(); return } catch(_: IllegalArgumentException) { stopSelf(); return }
            if(time-savedAt > 60000) { store.save(world); savedAt=time }
            val falling=world.pets.any { !it.motion.grounded && it.state!=State.DRAGGED }
            val active=world.interaction!=null || world.pets.any { it.state in listOf(State.WANDERING,State.APPROACHING,State.RECOVERING,State.REACTING) }
            val idleDelay=views.values.minOfOrNull { it.nextFrameDelay(time) } ?: 1000
            handler.postDelayed(this,if(falling || world.hearts.isNotEmpty()) 32 else if(active) 65 else idleDelay)
        }
    }
    private fun touch(view: PetView,who: Who) {
        var downX=0f; var downY=0f; var originalX=0f; var originalY=0f
        var velocity: VelocityTracker?=null
        var epoch=0
        val gesture=TouchHold(ViewConfiguration.get(this).scaledTouchSlop.toFloat(),ViewConfiguration.getLongPressTimeout().toLong())
        val hold=Runnable {
            if(epoch==gestureEpoch && screenOn && gesture.hold(now())) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showMenu(who)
            }
        }
        fun sample(event: MotionEvent) { val absolute=MotionEvent.obtain(event); absolute.offsetLocation(event.rawX-event.x,event.rawY-event.y); velocity?.addMovement(absolute); absolute.recycle() }
        view.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    removeMenu(); world.interrupt(now()); world.held=who; epoch=gestureEpoch
                    velocity?.recycle(); velocity=VelocityTracker.obtain(); sample(event)
                    downX=event.rawX; downY=event.rawY; originalX=world.pet(who).x; originalY=world.pet(who).y
                    gesture.down(now()); handler.postDelayed(hold,ViewConfiguration.getLongPressTimeout().toLong()); true
                }
                MotionEvent.ACTION_MOVE -> {
                    sample(event); val dx=event.rawX-downX; val dy=event.rawY-downY
                    if(epoch==gestureEpoch && gesture.move(dx,dy)) {
                        handler.removeCallbacks(hold); removeMenu(); world.held=null
                        world.command(Command.Drag(who,originalX+dx,originalY+dy),now())
                        val p=world.pet(who); val lp=params.getValue(who); lp.x=originX+p.x.toInt(); lp.y=originY+p.y.toInt()
                        try { wm.updateViewLayout(view,lp) } catch(_: Exception) { stopSelf() }
                        view.update(world,now()); removeCanopies(); removeSpeech(); removeProp()
                    }; true
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(hold); world.held=null
                    sample(event); velocity?.computeCurrentVelocity(1000)
                    val tap=gesture.end()
                    if(epoch==gestureEpoch) {
                        if(gesture.dragging) world.command(Command.Release(who,velocity?.xVelocity ?: 0f,velocity?.yVelocity ?: 0f),now())
                        else if(event.actionMasked==MotionEvent.ACTION_UP && tap) { view.performClick(); world.command(Command.Tap(who),now()) }
                    }
                    if(event.actionMasked==MotionEvent.ACTION_CANCEL) removeMenu()
                    velocity?.recycle(); velocity=null; store.save(world); handler.removeCallbacks(tick); handler.post(tick); true
                }
                else -> false
            }
        }
    }
    private fun showMenu(who: Who) {
        removeMenu(); removeSpeech()
        val view=ActionMenu(this,artwork.menus,who,{ removeMenu() }) { index ->
            removeMenu(); world.held=null
            world.choose(who,index,now()); store.save(world)
            handler.removeCallbacks(tick); handler.post(tick)
        }
        val lp=layout(minOf(dp(148),world.width.toInt()),minOf(dp(148),world.height.toInt()),false)
        lp.flags=lp.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        menu=view; menuWho=who; menuParams=lp; positionMenu(); wm.addView(view,lp)
        handler.postDelayed(dismissMenu,8000)
    }
    private fun positionMenu() {
        val lp=menuParams ?: return; val p=world.pet(menuWho ?: return)
        lp.x=originX+(p.x+world.petWidth/2-lp.width/2).toInt().coerceIn(0,(world.width.toInt()-lp.width).coerceAtLeast(0))
        lp.y=originY+(p.y-lp.height).toInt().coerceIn(0,(world.height.toInt()-lp.height).coerceAtLeast(0))
    }
    private fun removeMenu() {
        handler.removeCallbacks(dismissMenu)
        menu?.let { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }
        menu=null; menuWho=null; menuParams=null
    }
    private fun bounds(force: Boolean=false) {
        val metrics=wm.currentWindowMetrics
        val b=metrics.bounds
        val inset=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val w=b.width()-inset.left-inset.right
        val h=b.height()-inset.top-maxOf(inset.bottom,imeBottom)
        val pw=minOf(dp(72),w/3).coerceAtLeast(24); val ph=minOf(dp(104),h/3).coerceAtLeast(32)
        val key="$w,$h,$pw,$ph,${inset.left},${inset.top}"
        if(!force && key==geometry) return
        gestureEpoch++; world.held=null; removeCanopies(); removeMenu(); geometry=key; originX=inset.left; originY=inset.top
        world.resize(w.toFloat(),h.toFloat(),pw.toFloat(),ph.toFloat(),now()); removeSpeech(); removeProp()
        params.forEach { (who,lp) -> lp.width=pw; lp.height=ph; lp.x=originX+world.pet(who).x.toInt(); lp.y=originY+world.pet(who).y.toInt(); try { wm.updateViewLayout(views.getValue(who),lp) } catch(_: Exception) { stopSelf() } }
    }
    private fun showCanopies() {
        Who.entries.forEach { who ->
            val p=world.pet(who)
            if(p.state!=State.PARACHUTING) {
                canopies.remove(who)?.first?.let { if(it.isAttachedToWindow) wm.removeView(it) }
            } else {
                val existing=canopies[who]
                val pair=existing ?: (ParachuteView(this,artwork.parachutes,who) to layout(dp(96),dp(90),true))
                val lp=pair.second
                lp.alpha=.55f // Two overlapping pass-through canopies stay below Android’s .8 obscuring limit.
                val x=originX+(p.x+world.petWidth/2-lp.width/2).toInt().coerceIn(0,(world.width.toInt()-lp.width).coerceAtLeast(0))
                val y=originY+(p.y+world.petHeight*.34f-lp.height).toInt().coerceIn(0,(world.height.toInt()-lp.height).coerceAtLeast(0))
                val changed=lp.x!=x || lp.y!=y
                lp.x=x; lp.y=y
                if(existing==null) { canopies[who]=pair; wm.addView(pair.first,lp) }
                else if(changed) wm.updateViewLayout(pair.first,lp)
            }
        }
    }
    private fun removeCanopies() {
        canopies.values.forEach { if(it.first.isAttachedToWindow) try { wm.removeView(it.first) } catch(_: Exception) {} }
        canopies.clear()
    }
    private fun showSpeech() {
        if(world.pets.any { it.state==State.PARACHUTING }) { removeSpeech(); return }
        val bubble=world.bubble
        if(bubble==null) { removeSpeech(); return }
        if(spoken!=bubble) {
            removeSpeech()
            val text=BubbleView(this,artwork.bubbles,bubble.who,bubble.text)
            val lp=layout(minOf(dp(162),world.width.toInt()),dp(64),true)
            speech=text; speechParams=lp; spoken=bubble; positionSpeech(); wm.addView(text,lp)
        } else { positionSpeech(); speech?.let { wm.updateViewLayout(it,speechParams) } }
    }
    private fun positionSpeech() {
        val lp=speechParams ?: return; val p=world.pet(spoken?.who ?: return)
        lp.x=originX+(p.x+world.petWidth/2-lp.width/2).toInt().coerceIn(0,(world.width.toInt()-lp.width).coerceAtLeast(0))
        val bh=lp.height
        lp.y=originY+(if(p.y>bh) p.y.toInt()-bh else (p.y+world.petHeight).toInt()).coerceIn(0,(world.height.toInt()-bh).coerceAtLeast(0))
    }
    private fun removeSpeech() { speech?.let { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; speech=null; speechParams=null; spoken=null }
    private fun showProp(time: Long) {
        val prop=world.prop ?: run { removeProp(); return }
        val p=world.pet(prop.owner)
        val toLeft=world.facesLeft(prop.owner)
        val targetX=originX+p.x+world.petWidth*(if(toLeft) .28f else .65f)-dp(14)
        val targetY=originY+p.y+world.petHeight*(if(p.state==State.EATING) .36f else .56f)-dp(14)
        if(propView?.type!=prop.type) {
            removeProp(); val view=PropView(this,prop.type,artwork.props); val lp=layout(dp(22),dp(22),true)
            lp.x=targetX.toInt(); lp.y=targetY.toInt(); propView=view; propParams=lp; propOwner=prop.owner; propMovedAt=0
            wm.addView(view,lp)
        }
        val lp=propParams ?: return
        val scene=world.interaction
        propView?.setBitten(prop.type==PropType.COOKIE && scene!=null && (scene.stage>0 || time-scene.since>800))
        if(propOwner!=prop.owner) { propFromX=lp.x.toFloat(); propFromY=lp.y.toFloat(); propMovedAt=time; propOwner=prop.owner }
        val fraction=if(propMovedAt==0L) 1f else ((time-propMovedAt)/450f).coerceIn(0f,1f)
        val x=if(fraction>=1) targetX else propFromX+(targetX-propFromX)*fraction
        val y=if(fraction>=1) targetY else propFromY+(targetY-propFromY)*fraction-dp(12)*kotlin.math.sin(fraction*Math.PI).toFloat()
        val px=x.toInt().coerceIn(originX,(originX+world.width.toInt()-lp.width).coerceAtLeast(originX))
        val py=y.toInt().coerceIn(originY,(originY+world.height.toInt()-lp.height).coerceAtLeast(originY))
        if(lp.x!=px || lp.y!=py) { lp.x=px; lp.y=py; wm.updateViewLayout(propView,lp) }
    }
    private fun removeProp() { propView?.let { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; propView=null; propParams=null; propOwner=null }
    fun contextSignal(signal: ContextSignal) { world.command(Command.Context(signal),now()) }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); bounds() }
    override fun onDisplayChanged(displayId: Int) { bounds() }
    override fun onDisplayAdded(displayId: Int) { bounds() }
    override fun onDisplayRemoved(displayId: Int) { bounds() }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); if(::tilt.isInitialized) tilt.stop(); if(::store.isInitialized) store.save(world)
        removeCanopies(); removeMenu(); removeSpeech(); removeProp(); views.values.forEach { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; views.clear()
        if(started) { unregisterReceiver(receiver); displays.unregisterDisplayListener(this) }
        reference.clear(); super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder?=null
}
