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
    private val devPrefs by lazy { getSharedPreferences("dev_overrides",MODE_PRIVATE) }
    private var dev=DevOverrides(emptyMap<String,Int>())
    private var actualBattery: Int?=null
    private var actualGravity=GravityVector(0f,1f)
    private var actualReliable=false
    private var actualContext=ContextSignal.UNKNOWN
    private val refreshDev=Runnable {
        dev=DevOverrides(devPrefs.all)
        applyEnvironment(now())
        applyTilt(actualGravity,actualReliable)
        bounds()
        world.command(Command.Context(dev.context ?: actualContext),now())
        handler.removeCallbacks(tick); if(started && screenOn) handler.post(tick)
    }
    private val devListener=SharedPreferences.OnSharedPreferenceChangeListener { _,_ ->
        handler.removeCallbacks(refreshDev); handler.postDelayed(refreshDev,120)
    }
    fun previewIdle(who: Who,antic: IdleAntic): Boolean {
        val accepted=world.startIdle(who,antic,now())
        if(accepted) { handler.removeCallbacks(tick); handler.post(tick) }
        return accepted
    }
    fun previewShared(kind: Kind): Boolean {
        val accepted=world.start(kind,Who.HUSBAND,now())
        if(accepted) { handler.removeCallbacks(tick); handler.post(tick) }
        return accepted
    }
    private fun applyEnvironment(time: Long) {
        world.environment(null,System.currentTimeMillis(),dev.hour(java.time.LocalTime.now().hour),time,dev.kind,dev.temperature)
        world.batteryTemperature(dev.battery(actualBattery),time)
        nextEnvironment=time+60_000
    }
    private fun applyTilt(vector: GravityVector,reliable: Boolean) {
        val time=now(); val effective=dev.gravity ?: vector
        val turning=dev.angle?.let { orientation.turnTo(it,time) } ?: orientation.sample(vector,reliable,time)
        if(turning && world.interaction!=null) world.interrupt(time)
        world.gravity(effective.x,effective.y,time)
        if(screenOn && (turning || orientation.moving(time) || (!world.keyboardOpen && world.pets.any { it.state!=State.DRAGGED && !world.physics.supported(it,world.width,world.height,world.petWidth,world.petHeight) }))) {
            handler.removeCallbacks(tick); if(started) handler.post(tick)
        }
    }
    private lateinit var wm: WindowManager
    private lateinit var store: MoodStore
    private lateinit var displays: DisplayManager
    private lateinit var tilt: TiltSensor
    private var nextEnvironment=0L
    private val rainViews=mutableMapOf<Who,Pair<RainView,WindowManager.LayoutParams>>()
    private val views=mutableMapOf<Who,PetView>()
    private val overflowParams=mutableMapOf<Who,WindowManager.LayoutParams>()
    private val displayCenters=mutableMapOf<Who,Pair<Float,Float>>()
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
    private val keyboardSignals=KeyboardSignals()
    private val orientation=OrientationSnap()
    private var bodyWidth=0f; private var bodyHeight=0f
    private var usableForHeads=true
    private var savedAt=0L
    private var started=false
    private val receiver=object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    actualBattery=if(intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Int.MIN_VALUE) else null
                    world.batteryTemperature(dev.battery(actualBattery),now())
                    nextEnvironment=0L
                    if(started && screenOn) { handler.removeCallbacks(tick); handler.post(tick) }
                }
                Intent.ACTION_SCREEN_OFF -> { screenOn=false; tilt.stop(); handler.removeCallbacks(tick); world.held=null; gestureEpoch++; world.interrupt(now(),resetMotion=true); store.save(world); removeRain(); removeCanopies(); removeMenu(); removeSpeech(); removeProp(); views.values.forEach { it.visibility=View.GONE; it.overflow.visibility=View.GONE } }
                Intent.ACTION_SCREEN_ON,Intent.ACTION_USER_PRESENT -> { screenOn=true; ContextService.refresh(); tilt.start(); world.resetClock(); handler.removeCallbacks(tick); handler.post(tick) }
            }
        }
    }
    private fun now()=SystemClock.elapsedRealtime()
    private fun dp(v: Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate() {
        super.onCreate(); reference=java.lang.ref.WeakReference(this)
        dev=DevOverrides(devPrefs.all)
        wm=getSystemService(WindowManager::class.java); displays=getSystemService(DisplayManager::class.java); store=MoodStore(this); store.load(world)
        tilt=TiltSensor(this,{ displays.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0 }) { vector,reliable ->
            actualGravity=vector; actualReliable=reliable
            applyTilt(vector,reliable)
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
                view.setOnApplyWindowInsetsListener { _, insets ->
                    val next=if(insets.isVisible(WindowInsets.Type.ime())) insets.getInsets(WindowInsets.Type.ime()).bottom else 0
                    if(next!=keyboardSignals.insetBottom) { keyboardSignals.insetBottom=next; handler.post { bounds() } }; insets
                }
                wm.addView(view,lp)
                val extra=layout(dp(160),dp(240),true); extra.alpha=.05f
                extra.flags=extra.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                overflowParams[who]=extra; wm.addView(view.overflow,extra)
            }
            ContextService.keyboard?.let { keyboardSignals.windowVisible=it.first; keyboardSignals.windowTop=it.second }
            bounds()
            world.pets.forEachIndexed { index,p -> p.x=world.width*(if(index==0) .16f else .60f); p.y=(world.height-world.petHeight-dp(45)).coerceAtLeast(0f) }
            bounds(force=true)
        } catch(_: Exception) { stopSelf(); return }
        val filter=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_BATTERY_CHANGED) }
        if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(receiver,filter)
        displays.registerDisplayListener(this,handler); started=true
        ContextService.refresh()
        devPrefs.registerOnSharedPreferenceChangeListener(devListener)
        applyEnvironment(now()); applyTilt(actualGravity,actualReliable)
        screenOn=getSystemService(PowerManager::class.java).isInteractive
        if(screenOn) { tilt.start(); handler.post(tick) }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    private fun layout(w: Int,h: Int,passThrough: Boolean)=WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if(passThrough) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0),PixelFormat.TRANSLUCENT).apply {
        gravity=Gravity.TOP or Gravity.LEFT
        // Bubbles remain below Android's maximum obscuring opacity for pass-through touches.
        if(passThrough) alpha=.01f
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
            if(locked) { world.held=null; gestureEpoch++; removeRain(); removeCanopies(); removeMenu(); tilt.stop(); views.values.forEach { it.visibility=View.GONE; it.overflow.visibility=View.GONE }; removeSpeech(); removeProp(); handler.postDelayed(this,2000); return }
            tilt.start()
            views.values.forEach { it.visibility=if(usableForHeads) View.VISIBLE else View.GONE; it.overflow.visibility=it.visibility }
            val time=now()
            if(dev.angle==null) orientation.sample(actualGravity,actualReliable,time)
            val angle=orientation.value(time)
            if(time>=nextEnvironment) {
                applyEnvironment(time)
            }
            val size=OrientationSnap.extent(bodyWidth,bodyHeight,angle)
            world.footprint(size.first,size.second)
            world.tick(time)
            try {
                world.pets.forEach { p -> updatePetWindow(p,time,angle) }
                if(menu==null) showSpeech() else { removeSpeech(); positionMenu(); menu?.let { wm.updateViewLayout(it,menuParams) } }
                showProp(time)
                showCanopies()
                showRain(time,angle)
                balancePassThrough()
            } catch(_: WindowManager.BadTokenException) { stopSelf(); return } catch(_: SecurityException) { stopSelf(); return } catch(_: IllegalArgumentException) { stopSelf(); return }
            if(time-savedAt > 60000) { store.save(world); savedAt=time }
            val falling=world.pets.any { !it.motion.grounded && it.state!=State.DRAGGED }
            val active=world.idleMoving || world.interaction!=null || world.pets.any { it.state in listOf(State.WANDERING,State.APPROACHING,State.RECOVERING,State.REACTING) }
            val idleDelay=views.values.minOfOrNull { it.nextFrameDelay(time) } ?: 1000
            handler.postDelayed(this,if((world.weather.raining && !world.keyboardOpen) || world.returningFromKeyboard || orientation.moving(time) || world.pets.any { it.state==State.RETREATING } || (!world.keyboardOpen && falling) || world.hearts.isNotEmpty()) 32 else if(world.deviceHeat.hot && !world.keyboardOpen) 80 else if(active) 65 else idleDelay)
        }
    }
    private fun touch(view: PetView,who: Who) {
        var downX=0f; var downY=0f; var originalX=0f; var originalY=0f
        var velocity: VelocityTracker?=null
        var epoch=0
        val gesture=TouchHold(ViewConfiguration.get(this).scaledTouchSlop.toFloat(),ViewConfiguration.getLongPressTimeout().toLong())
        val hold=Runnable {
            if(epoch==gestureEpoch && screenOn && !world.keyboardOpen && gesture.hold(now())) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showMenu(who)
            }
        }
        fun sample(event: MotionEvent) { val absolute=MotionEvent.obtain(event); absolute.offsetLocation(event.rawX-event.x,event.rawY-event.y); velocity?.addMovement(absolute); absolute.recycle() }
        view.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if(world.keyboardOpen) return@setOnTouchListener true
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
                        try { updatePetWindow(world.pet(who),now(),orientation.value(now())) } catch(_: Exception) { stopSelf() }
                        removeCanopies(); removeSpeech(); removeProp()
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
        lp.x=originX+(face(p.who).first-lp.width/2).toInt().coerceIn(0,(world.width.toInt()-lp.width).coerceAtLeast(0))
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
        imeBottom=dev.keyboard?.let { if(it) (b.height()*.40f).toInt() else 0 } ?: keyboardSignals.bottom(b.height())
        val keyboardVisible=dev.keyboard ?: keyboardSignals.visible
        val h=(b.height()-inset.top-maxOf(inset.bottom,imeBottom)).coerceAtLeast(1)
        usableForHeads=h>=dp(60)
        // Fold/IME changes move the companions; they must never change their scale.
        val pw=dp(72); val ph=dp(104)
        val key="$w,$h,$pw,$ph,${inset.left},${inset.top},$keyboardVisible"
        if(!force && key==geometry) return
        gestureEpoch++; world.held=null; removeRain(); removeCanopies(); removeMenu(); geometry=key; originX=inset.left; originY=inset.top
        bodyWidth=pw.toFloat(); bodyHeight=ph.toFloat()
        val size=OrientationSnap.extent(bodyWidth,bodyHeight,orientation.value(now()))
        world.resize(w.toFloat(),h.toFloat(),size.first,size.second,now())
        world.keyboard(keyboardVisible,now()); removeSpeech(); removeProp()
        world.pets.forEach { updatePetWindow(it,now(),orientation.value(now())) }
        handler.removeCallbacks(tick); if(started && screenOn) handler.post(tick)
    }
    fun keyboardWindow(visible: Boolean?,top: Int?) {
        keyboardSignals.windowVisible=visible; keyboardSignals.windowTop=top
        if(::wm.isInitialized) bounds()
    }
    private fun updatePetWindow(p: Pet,time: Long,angle: Float) {
        val lp=params.getValue(p.who); val view=views.getValue(p.who)
        val peek=p.state==State.PEEKING
        view.orientation(bodyWidth,bodyHeight,angle); view.update(world,time)
        val unit=bodyHeight/104f
        val size=if(peek) OrientationSnap.extent(72*unit,72*unit,angle) else world.petWidth to world.petHeight
        fun even(v: Float)=2*kotlin.math.ceil(v/2).toInt()
        val w=even(size.first); val h=even(size.second)
        val ink=view.contentBounds(angle)
        val cx=CharacterGeometry.visibleCenter(if(peek) { if(p.hideLeft) w/2f else world.width-w/2f } else p.x+world.petWidth/2,minOf(ink[0],-w/2f),maxOf(ink[2],w/2f),world.width)
        val cy=CharacterGeometry.visibleCenter(if(peek) world.peekTop(p.who,h.toFloat())+h/2 else p.y+world.petHeight/2,minOf(ink[1],-h/2f),maxOf(ink[3],h/2f),world.height)
        // One integer center for both surfaces avoids a fractional-pixel seam.
        val centerX=kotlin.math.round(cx); val centerY=kotlin.math.round(cy)
        displayCenters[p.who]=centerX to centerY
        val x=originX+centerX.toInt()-w/2; val y=originY+centerY.toInt()-h/2
        val flags=if(world.keyboardOpen) lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        val alpha=if(world.keyboardOpen) { if(lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE!=0) lp.alpha else .01f } else 1f
        val changed=lp.x!=x || lp.y!=y || lp.width!=w || lp.height!=h || lp.flags!=flags || lp.alpha!=alpha
        lp.x=x; lp.y=y; lp.width=w; lp.height=h; lp.flags=flags; lp.alpha=alpha
        if(changed) wm.updateViewLayout(view,lp)
        val extra=overflowParams.getValue(p.who)
        val extent=OrientationSnap.extent(CharacterGeometry.OVERFLOW_WIDTH*unit,CharacterGeometry.OVERFLOW_HEIGHT*unit,angle)
        val ew=even(extent.first); val eh=even(extent.second)
        val ex=originX+centerX.toInt()-ew/2; val ey=originY+centerY.toInt()-eh/2
        if(extra.x!=ex || extra.y!=ey || extra.width!=ew || extra.height!=eh) {
            extra.x=ex; extra.y=ey; extra.width=ew; extra.height=eh; wm.updateViewLayout(view.overflow,extra)
        }
    }
    private fun center(who: Who)=displayCenters[who] ?: (world.pet(who).x+world.petWidth/2 to world.pet(who).y+world.petHeight/2)
    private fun face(who: Who): Pair<Float,Float> {
        val f=views.getValue(who).facePosition(); val r=Math.toRadians(orientation.value(now()).toDouble())
        val c=center(who)
        return c.first+(f.first*kotlin.math.cos(r)-f.second*kotlin.math.sin(r)).toFloat() to c.second+(f.first*kotlin.math.sin(r)+f.second*kotlin.math.cos(r)).toFloat()
    }
    /** Conservative global opacity budget, including overlapping spouses and effects. */
    private fun balancePassThrough() {
        val layers=mutableListOf<Triple<View,WindowManager.LayoutParams,Float>>()
        Who.entries.forEach { who ->
            overflowParams[who]?.let { layers.add(Triple(views.getValue(who).overflow,it,.65f)) }
            if(world.keyboardOpen) params[who]?.let { layers.add(Triple(views.getValue(who),it,.55f)) }
        }
        rainViews.values.forEach { layers.add(Triple(it.first,it.second,.30f)) }
        canopies.values.forEach { layers.add(Triple(it.first,it.second,.55f)) }
        speech?.let { v -> speechParams?.let { layers.add(Triple(v,it,.85f)) } }
        propView?.let { v -> propParams?.let { layers.add(Triple(v,it,.75f)) } }
        val alphas=OverlayOpacity.distribute(layers.map { it.third })
        layers.forEachIndexed { index,(view,lp,_) ->
            val alpha=alphas[index]
            if(kotlin.math.abs(lp.alpha-alpha)>.001f) { lp.alpha=alpha; wm.updateViewLayout(view,lp) }
        }
    }

    private fun showRain(time: Long,angle: Float) {
        if(!world.weather.raining || world.keyboardOpen || !usableForHeads) { removeRain(); return }
        Who.entries.forEach { who ->
            val p=world.pet(who)
            val existing=rainViews[who]
            val pair=existing ?: (RainView(this) to layout(dp(160),dp(260),true))
            val size=OrientationSnap.extent(dp(160).toFloat(),dp(260).toFloat(),angle)
            val lp=pair.second
            val w=size.first.toInt(); val h=size.second.toInt()
            val x=originX+(center(who).first-w/2).toInt()
            val y=originY+(center(who).second-h/2).toInt()
            val changed=lp.x!=x || lp.y!=y || lp.width!=w || lp.height!=h
            lp.x=x; lp.y=y; lp.width=w; lp.height=h
            pair.first.update(p,time,angle,umbrellaPose(world.weather.raining,world.pose(who,time).frame),views.getValue(who).rainSurface())
            if(existing==null) { rainViews[who]=pair; wm.addView(pair.first,lp) }
            else if(changed) wm.updateViewLayout(pair.first,lp)
        }
    }
    private fun removeRain() {
        rainViews.values.forEach { it.first.particles.clear(); try { wm.removeViewImmediate(it.first) } catch(_: Exception) {} }
        rainViews.clear()
    }
    private fun showCanopies() {
        if(world.keyboardOpen) { removeCanopies(); return }
        Who.entries.forEach { who ->
            val p=world.pet(who)
            if(!canopyVisible(p,world.physics,world.width,world.height,world.petWidth,world.petHeight) || world.held==who) {
                canopies.remove(who)?.first?.let { it.expire(); try { wm.removeViewImmediate(it) } catch(_: IllegalArgumentException) {} }
            } else {
                val existing=canopies[who]
                val pair=existing ?: (ParachuteView(this,artwork.parachutes,who) to layout(dp(96),dp(90),true))
                val lp=pair.second
                val angle=orientation.value(now()); pair.first.renew(now()); pair.first.orientation(angle)
                val extent=OrientationSnap.extent(dp(96).toFloat(),dp(90).toFloat(),angle)
                val w=extent.first.toInt(); val h=extent.second.toInt()
                val radians=Math.toRadians(angle.toDouble())
                val distance=-views.getValue(who).facePosition().second+dp(35)
                val x=originX+(center(who).first+kotlin.math.sin(radians)*distance-w/2).toInt().coerceIn(0,(world.width.toInt()-w).coerceAtLeast(0))
                val y=originY+(center(who).second-kotlin.math.cos(radians)*distance-h/2).toInt().coerceIn(0,(world.height.toInt()-h).coerceAtLeast(0))
                val changed=lp.x!=x || lp.y!=y || lp.width!=w || lp.height!=h
                lp.width=w; lp.height=h
                lp.x=x; lp.y=y
                if(existing==null) { canopies[who]=pair; wm.addView(pair.first,lp) }
                else if(changed) wm.updateViewLayout(pair.first,lp)
            }
        }
    }
    private fun removeCanopies() {
        canopies.values.forEach { it.first.expire(); try { wm.removeViewImmediate(it.first) } catch(_: Exception) {} }
        canopies.clear()
    }
    private fun showSpeech() {
        if(world.weather.raining || world.keyboardOpen || world.pets.any { it.state==State.PARACHUTING }) { removeSpeech(); return }
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
        lp.x=originX+(face(p.who).first-lp.width/2).toInt().coerceIn(0,(world.width.toInt()-lp.width).coerceAtLeast(0))
        val bh=lp.height
        lp.y=originY+(if(face(p.who).second>bh+dp(22)) face(p.who).second.toInt()-bh-dp(22) else face(p.who).second.toInt()+dp(22)).coerceIn(0,(world.height.toInt()-bh).coerceAtLeast(0))
    }
    private fun removeSpeech() { speech?.let { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; speech=null; speechParams=null; spoken=null }
    private fun showProp(time: Long) {
        if(world.keyboardOpen) { removeProp(); return }
        val prop=world.prop ?: run { removeProp(); return }
        val p=world.pet(prop.owner)
        val toLeft=world.facesLeft(prop.owner)
        val angle=orientation.value(time); val r=Math.toRadians(angle.toDouble())
        val extent=OrientationSnap.extent(dp(22).toFloat(),dp(22).toFloat(),angle)
        val faceLocal=views.getValue(prop.owner).facePosition()
        val localX=faceLocal.first+bodyWidth*(if(toLeft) -.08f else .08f)
        val localY=faceLocal.second+bodyHeight*(if(p.state==State.EATING) .08f else .25f)
        val targetX=originX+center(prop.owner).first+(localX*kotlin.math.cos(r)-localY*kotlin.math.sin(r)).toFloat()-extent.first/2
        val targetY=originY+center(prop.owner).second+(localX*kotlin.math.sin(r)+localY*kotlin.math.cos(r)).toFloat()-extent.second/2
        if(propView?.type!=prop.type) {
            removeProp(); val view=PropView(this,prop.type,artwork.props); val lp=layout(dp(22),dp(22),true)
            lp.x=targetX.toInt(); lp.y=targetY.toInt(); propView=view; propParams=lp; propOwner=prop.owner; propMovedAt=0
            wm.addView(view,lp)
        }
        val lp=propParams ?: return
        propView?.orientation(angle)
        val sizeChanged=lp.width!=extent.first.toInt() || lp.height!=extent.second.toInt()
        lp.width=extent.first.toInt(); lp.height=extent.second.toInt()
        val scene=world.interaction
        propView?.setBitten(prop.type==PropType.COOKIE && scene!=null && (scene.stage>0 || time-scene.since>800))
        if(propOwner!=prop.owner) { propFromX=lp.x.toFloat(); propFromY=lp.y.toFloat(); propMovedAt=time; propOwner=prop.owner }
        val fraction=if(propMovedAt==0L) 1f else ((time-propMovedAt)/450f).coerceIn(0f,1f)
        val x=if(fraction>=1) targetX else propFromX+(targetX-propFromX)*fraction
        val y=if(fraction>=1) targetY else propFromY+(targetY-propFromY)*fraction-dp(12)*kotlin.math.sin(fraction*Math.PI).toFloat()
        val px=x.toInt().coerceIn(originX,(originX+world.width.toInt()-lp.width).coerceAtLeast(originX))
        val py=y.toInt().coerceIn(originY,(originY+world.height.toInt()-lp.height).coerceAtLeast(originY))
        if(sizeChanged || lp.x!=px || lp.y!=py) { lp.x=px; lp.y=py; wm.updateViewLayout(propView,lp) }
    }
    private fun removeProp() { propView?.let { if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; propView=null; propParams=null; propOwner=null }
    fun contextSignal(signal: ContextSignal) { actualContext=signal; world.command(Command.Context(dev.context ?: signal),now()) }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); bounds() }
    override fun onDisplayChanged(displayId: Int) { bounds() }
    override fun onDisplayAdded(displayId: Int) { bounds() }
    override fun onDisplayRemoved(displayId: Int) { bounds() }
    override fun onDestroy() {
        devPrefs.unregisterOnSharedPreferenceChangeListener(devListener)
        handler.removeCallbacksAndMessages(null); if(::tilt.isInitialized) tilt.stop(); if(::store.isInitialized) store.save(world)
        removeRain(); removeCanopies(); removeMenu(); removeSpeech(); removeProp(); views.values.forEach { try { wm.removeViewImmediate(it.overflow) } catch(_: Exception) {}; if(it.isAttachedToWindow) try { wm.removeView(it) } catch(_: Exception) {} }; views.clear(); overflowParams.clear(); displayCenters.clear()
        if(started) { unregisterReceiver(receiver); displays.unregisterDisplayListener(this) }
        reference.clear(); super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder?=null
}
