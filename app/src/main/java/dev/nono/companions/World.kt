package dev.nono.companions

import kotlin.math.abs
import kotlin.random.Random

enum class Who { HUSBAND, WIFE; fun partner() = if (this == HUSBAND) WIFE else HUSBAND }
enum class State { IDLE, RETREATING, PEEKING, OBSERVING, WANDERING, RESTING, FALLING, PARACHUTING, DRAGGED, RECOVERING, EATING, APPROACHING, SHARED, REACTING, EXERCISING, FIXING, COOLING, IDLE_ANTIC }
enum class Pose(val frame: Int) { IDLE(4), WALK(0), EAT(24), CLAW(12), REST(28), SURPRISE(23), HUG(16), ANTIC(32), FALL(8), LAND(10), REACH(20), SMIRK(15), WINK(19), FIX(34), KISS_ENTER(36), KISS(38), KISS_AFTER(39), PARACHUTE(40), PEEK(42), FAN(44), PLACE_FAN(46), BREEZE(47), FEET(48), STRETCH(52), SIGNATURE(56), RUN(60) }
enum class Kind { SNACK, DINO, AFFECTION, PERSONAL, KISS, FOOT_DUET, COPY_STRETCH, TAG }
enum class ContextSignal { UNKNOWN, READING, GAME, MEDIA, WORK }
enum class PropType { COOKIE, TOOL, DARK_HEART }
data class Needs(var hunger: Float = 45f, var energy: Float = 75f, var affection: Float = 65f, var mischief: Float = 55f) {
    fun bound() { hunger = safe(hunger); energy = safe(energy); affection = safe(affection); mischief = safe(mischief) }
    private fun safe(v: Float) = if (v.isFinite()) v.coerceIn(0f, 100f) else 50f
}
data class Pet(val who: Who, var x: Float, var y: Float, val needs: Needs = Needs(), var state: State = State.IDLE, var until: Long = 0, var target: Float = x, val motion: Motion=Motion(), var idleAccentUntil: Long=0, var nextAccent: Long=0, var facing: Int=1, var hideLeft: Boolean=true, var peekAnchor: Float=.5f)
data class Prop(val type: PropType, var owner: Who, val expires: Long)
data class Bubble(val who: Who, val text: String, val until: Long)
data class Interaction(val kind: Kind, val leader: Who, var stage: Int, var since: Long, val started: Long)
sealed class Command {
    data class Tap(val who: Who) : Command()
    data class Drag(val who: Who, val x: Float, val y: Float) : Command()
    data class Release(val who: Who, val vx: Float=0f, val vy: Float=0f) : Command()
    data class Play(val kind: Kind, val leader: Who) : Command()
    data class Idle(val who: Who, val antic: IdleAntic) : Command()
    data class Rest(val who: Who) : Command()
    data class Context(val signal: ContextSignal) : Command()
}

/** Pure deterministic simulation. Commands are source-neutral; no transport or Android dependencies. */
class World(private val random: Random = Random.Default) {
    val pets = arrayOf(Pet(Who.HUSBAND, 25f, 300f), Pet(Who.WIFE, 180f, 300f))
    var width = 400f; private set
    var height = 700f; private set
    var petWidth = 100f; private set
    var petHeight = 150f; private set
    var bond = 25f
    val physics=GravityPhysics()
    val weather=WeatherState()
    val deviceHeat=DeviceHeat()
    val idleLife=Array(2) { IdleLife() }
    val idleMoving get()=idleLife.any { it.performance?.antic==IdleAntic.RUN }
    val cooling=Array(2) { CoolingController() }
    private var environmentReading: WeatherReading?=null
    private var environmentWall=0L
    private var environmentHour=12
    private var environmentKind: WeatherKind?=null
    private var environmentTemp: Float?=null
    private var nextWeatherSpeech=0L
    val hearts=mutableListOf<KissHeart>()
    private var nextHeart=0L
    var keyboardOpen=false; private set
    var returningFromKeyboard=false; private set
    var held: Who?=null
    var prop: Prop? = null
    var bubble: Bubble? = null
    var interaction: Interaction? = null; private set
    private val cooldowns = mutableMapOf<Kind, Long>()
    private var last = 0L
    private var nextChoice = 0L
    private var nextSpeech = 0L
    private var nextContext = 0L
    private var lastText = ""
    fun pet(who: Who) = pets[who.ordinal]
    fun facesLeft(who: Who): Boolean { val p=pet(who); return if(p.state in listOf(State.WANDERING,State.APPROACHING,State.RETREATING,State.IDLE_ANTIC) || (interaction?.kind==Kind.TAG && interaction?.stage==2)) p.facing<0 else pet(who.partner()).x<p.x }
    fun resize(w: Float, h: Float, pw: Float, ph: Float, now: Long) {
        interrupt(now,resetMotion=true)
        width = w.coerceAtLeast(1f); height = h.coerceAtLeast(1f)
        petWidth = pw; petHeight = ph
        pets.forEach { clamp(it); it.target = it.x; it.motion.vx=0f; it.motion.vy=0f; it.motion.grounded=physics.supported(it,width,height,petWidth,petHeight) }
    }
    fun footprint(pw: Float,ph: Float) {
        if(pw==petWidth && ph==petHeight) return
        val dx=(petWidth-pw)/2; val dy=(petHeight-ph)/2
        petWidth=pw; petHeight=ph
        pets.forEach { it.x+=dx; it.y+=dy; clamp(it) }
    }
    fun keyboard(open: Boolean,now: Long) {
        if(open==keyboardOpen) return
        keyboardOpen=open; returningFromKeyboard=!open; held=null; interrupt(now,resetMotion=true)
        if(open) pets.forEach {
            it.hideLeft=it.x+petWidth/2<=width/2
            it.peekAnchor=((it.y+petHeight/2)/height).coerceIn(0f,1f)
            it.state=State.RETREATING
        }
        else pets.forEach { it.state=State.WANDERING; it.motion.grounded=true; it.target=width*(if(it.who==Who.HUSBAND) .22f else .68f) }
    }
    /** Screen-space slots: when sharing an edge, she peeks just beneath him. */
    fun peekTop(who: Who,headHeight: Float): Float {
        val available=(height-headHeight).coerceAtLeast(0f)
        if(pets[0].hideLeft!=pets[1].hideLeft) return (pet(who).peekAnchor*height-headHeight/2).coerceIn(0f,available)
        val gap=minOf(headHeight*.85f,available)
        val top=(pet(Who.HUSBAND).peekAnchor*height-headHeight/2).coerceIn(0f,(available-gap).coerceAtLeast(0f))
        return top+if(who==Who.WIFE) gap else 0f
    }
    fun gravity(x: Float,y: Float,now: Long) {
        if(physics.setGravity(x,y)) { if(interaction!=null || idleLife.any { it.performance!=null }) interrupt(now); pets.forEach { it.motion.grounded=false } }
    }
    private fun clamp(p: Pet) { p.x = p.x.coerceIn(0f, (width - petWidth).coerceAtLeast(0f)); p.y = p.y.coerceIn(0f, (height - petHeight).coerceAtLeast(0f)) }
    fun command(c: Command, now: Long) {
        if(keyboardOpen) return
        if(c is Command.Drag && (!c.x.isFinite() || !c.y.isFinite())) return
        if(c is Command.Drag || c is Command.Tap) returningFromKeyboard=false
        when(c) {
            is Command.Drag -> { interrupt(now); pet(c.who).apply { state = State.DRAGGED; motion.fallSince=-1; x = c.x; y = c.y; clamp(this) } }
            is Command.Release -> pet(c.who).apply { if (state == State.DRAGGED) { state = State.RECOVERING; until = now + 700; motion.vx=(if(c.vx.isFinite()) c.vx else 0f).coerceIn(-petHeight*5,petHeight*5); motion.vy=(if(c.vy.isFinite()) c.vy else 0f).coerceIn(-petHeight*5,petHeight*5); motion.grounded=false; motion.fallSince=now; say(who, if(who == Who.HUSBAND) "Chute calculée !" else "Rattrape-moi !", now) } }
            is Command.Tap -> { if(pet(c.who).state != State.DRAGGED) { interrupt(now); pet(c.who).apply { state = State.REACTING; until = now + 2400; needs.affection += 2; needs.bound() }; say(c.who, if(c.who == Who.HUSBAND) listOf("Inspection du goûter ?", "Je gère.", "Encore faim…", "Un câlin, puis un biscuit ?")[random.nextInt(4)] else listOf("Qui, moi ?", "Il a l’air bon, ton goûter.", "Viens par ici.", "Bisou… et ton biscuit.")[random.nextInt(4)], now) } }
            is Command.Play -> start(c.kind, c.leader, now)
            is Command.Idle -> startIdle(c.who,c.antic,now)
            is Command.Rest -> { interrupt(now); pet(c.who).apply { state=State.RESTING; until=now+18000 }; say(c.who,"Petite sieste…",now) }
            is Command.Context -> if(now >= nextContext && interaction == null && idleLife.all { it.performance==null } && c.signal != ContextSignal.UNKNOWN) {
                nextContext = now + 120000
                pet(Who.HUSBAND).apply { state = State.OBSERVING; until = now + 3500 }
                say(Who.HUSBAND, when(c.signal) { ContextSignal.GAME -> "Le joueur deux a faim."; ContextSignal.READING -> "Je lis avec toi."; ContextSignal.MEDIA -> "J’ai les provisions !"; else -> "Concentre-toi, je gère." }, now)
            }
        }
    }
    /** Local menus share the same validated scene entry point as other command sources. */
    fun choose(who: Who,index: Int,now: Long): Boolean {
        if(keyboardOpen) return false
        if(index !in 0..3 || pets.any { it.state==State.DRAGGED }) return false
        if(index==3) { command(Command.Rest(who),now); return true }
        val kind=listOf(Kind.SNACK,Kind.KISS,Kind.DINO)[index]
        if(pets.any { !physics.supported(it,width,height,petWidth,petHeight) }) {
            say(who,"Je me pose d’abord !",now); return false
        }
        if(now<(cooldowns[kind] ?: 0)) { say(who,"Dans un petit instant…",now); return false }
        interrupt(now)
        pets.forEach { it.state=State.IDLE }
        return start(kind,if(index==0 && who==Who.WIFE) who.partner() else who,now)
    }
    fun start(kind: Kind, leader: Who, now: Long): Boolean {
        if(keyboardOpen || deviceHeat.hot || held!=null) return false
        if(kind in listOf(Kind.FOOT_DUET,Kind.COPY_STRETCH,Kind.TAG) && (physics.gravity.y<.7f || width<petWidth*2.2f)) return false
        if(kind==Kind.TAG && (weather.raining || weather.outfit==Outfit.NIGHT || pets.any { it.needs.energy<40 })) return false
        // The grounded kissing poses need a common floor; retry after the phone is upright.
        if(kind==Kind.KISS && (physics.gravity.y<.7f || pets.any { abs(it.y-(height-petHeight))>1f })) return false
        if(interaction != null || pets.any { it.state == State.DRAGGED || it.state == State.RECOVERING || !physics.supported(it,width,height,petWidth,petHeight) } || now < (cooldowns[kind] ?: 0)) return false
        idleLife.forEach { it.cancel(now) }
        interaction = Interaction(kind, leader, 0, now, now)
        pets.forEach { it.state = State.SHARED }
        when(kind) {
            Kind.SNACK -> { prop = Prop(PropType.COOKIE, leader, now + 26000); pet(leader).state = State.EATING; say(leader, if(leader == Who.HUSBAND) "Enfin un goûter !" else "Celui-là est à moi…", now) }
            Kind.DINO -> { say(leader, "dinsoauurr...", now) }
            Kind.FOOT_DUET, Kind.COPY_STRETCH, Kind.TAG -> { pet(leader).state=State.APPROACHING }
            Kind.AFFECTION, Kind.KISS -> { pet(leader).state = State.APPROACHING }
            Kind.PERSONAL -> {
                pet(leader).state = if(leader == Who.HUSBAND) if(random.nextBoolean()) State.EXERCISING else State.FIXING else State.SHARED
                prop = if(pet(leader).state==State.EXERCISING) null else Prop(if(leader == Who.HUSBAND) PropType.TOOL else PropType.DARK_HEART, leader, now + 16000)
            }
        }
        return true
    }
    fun interrupt(now: Long, resetMotion: Boolean=false) {
        interaction?.let { cooldowns[it.kind] = now + 25000 }
        interaction = null; prop = null; bubble = null; hearts.clear()
        cooling.forEach { it.cancel(now) }
        idleLife.forEach { it.cancel(now) }
        pets.forEach {
            if(resetMotion || it.state !in listOf(State.FALLING,State.PARACHUTING)) {
                it.state=State.RECOVERING; it.until=now+900; it.motion.fallSince=-1
            }
        }
        nextChoice = now + 12000
    }
    fun tick(now: Long) {
        val dt = if(last == 0L) 0f else ((now-last).coerceIn(0, 1000) / 1000f)
        last = now
        if(keyboardOpen) {
            pets.forEach { p ->
                val target=if(p.hideLeft) 0f else (width-petWidth).coerceAtLeast(0f)
                val step=petHeight*4*dt
                p.facing=if(target<p.x) -1 else 1
                p.x+=(target-p.x).coerceIn(-step,step)
                p.y=p.y.coerceIn(0f,(height-petHeight).coerceAtLeast(0f))
                p.motion.vx=0f; p.motion.vy=0f; p.motion.fallSince=-1
                p.state=if(abs(p.x-target)<2) State.PEEKING else State.RETREATING
            }
            return
        }
        if(returningFromKeyboard) {
            pets.forEach { p ->
                val target=(width*(if(p.who==Who.HUSBAND) .22f else .68f)).coerceIn(0f,(width-petWidth).coerceAtLeast(0f))
                p.facing=if(target<p.x) -1 else 1
                p.x+=(target-p.x).coerceIn(-petHeight*2*dt,petHeight*2*dt)
                physics.step(p,width,height,petWidth,petHeight,dt,now)
                if(p.motion.grounded) p.state=State.WANDERING
            }
            if(pets.all { abs(it.x-(width*(if(it.who==Who.HUSBAND) .22f else .68f)).coerceIn(0f,(width-petWidth).coerceAtLeast(0f)))<2 }) {
                returningFromKeyboard=false; pets.filter { it.motion.grounded }.forEach { it.state=State.IDLE }
            }
            return
        }
        pets.forEach { p -> p.needs.apply { hunger += dt * .035f; energy += dt * if(p.state == State.RESTING) .12f else -.012f; bound() }
            if(p.state in listOf(State.RECOVERING, State.REACTING, State.OBSERVING, State.RESTING) && now >= p.until) p.state = State.IDLE
            if(p.state == State.WANDERING && interaction == null && p.motion.grounded) { move(p, p.target, p.y, dt); if(abs(p.x-p.target)<2) p.state = State.IDLE }
            if(held!=p.who) physics.step(p,width,height,petWidth,petHeight,dt,now)
            if(p.state==State.IDLE && now>=p.nextAccent) { p.idleAccentUntil=now+1300; p.nextAccent=now+random.nextLong(6000,14000) }
        }
        if(held==null && interaction==null && idleLife.all { it.performance==null } && pets.all { it.motion.grounded && it.state!=State.DRAGGED && it.state!=State.COOLING }) {
            val a=pets[0]; val b=pets[1]
            val vertical=abs(physics.gravity.x)>abs(physics.gravity.y)
            val separation=if(vertical) petHeight*.72f else petWidth*.72f
            val delta=if(vertical) b.y-a.y else b.x-a.x
            if(abs(delta)<separation) {
                val nudge=minOf((separation-abs(delta))/2,petHeight*dt)
                val direction=if(delta>=0) 1 else -1
                if(vertical) { a.y-=nudge*direction; b.y+=nudge*direction } else { a.x-=nudge*direction; b.x+=nudge*direction }
                clamp(a); clamp(b)
            }
        }
        hearts.removeAll { now-it.born>=it.life }
        if(interaction?.kind==Kind.KISS && interaction?.stage==2 && now>=nextHeart) {
            Who.entries.forEach { hearts.add(KissHeart(it,now,random.nextLong(400,901),random.nextFloat()*.5f+.25f,random.nextFloat()*.17f+.16f)) }
            nextHeart=now+150
        }
        bubble?.let { if(now >= it.until) bubble = null }
        prop?.let { if(now >= it.expires) prop = null }
        pets.forEach { p ->
            val controller=cooling[p.who.ordinal]
            val eligible=held==null && interaction==null && p.motion.grounded && p.state in listOf(State.IDLE,State.COOLING)
            controller.tick(deviceHeat.hot,eligible,now)
            if(controller.phase!=CoolingPhase.NONE) p.state=State.COOLING
            else if(p.state==State.COOLING) p.state=State.IDLE
        }
        advanceIdles(now,dt)
        val joint = interaction
        if(joint != null) { if(pets.any { !it.motion.grounded }) interrupt(now) else advance(joint, now, dt) }
        else if(!deviceHeat.hot && held==null && now >= nextChoice && pets.none { it.state == State.DRAGGED || it.state == State.RECOVERING || it.state==State.IDLE_ANTIC || !it.motion.grounded }) {
            nextChoice = now + random.nextLong(9000, 19000)
            val p = pets[random.nextInt(2)]
            if(p.needs.energy < 30 || random.nextFloat() < .22f) { p.state = State.RESTING; p.until = now + 18000 }
            else if(random.nextFloat() < .52f) {
                val quiet=weather.raining || weather.outfit==Outfit.NIGHT
                idleLife[p.who.ordinal].choose(p.needs,quiet,now,random)?.let { startIdle(p.who,it,now) }
            }
            else if(random.nextFloat() < .3f) { p.state = State.WANDERING; p.target = random.nextFloat() * (width-petWidth).coerceAtLeast(0f) }
            else {
                val weights = listOf(Kind.SNACK to (p.needs.hunger + pet(p.who.partner()).needs.mischief), Kind.DINO to 35f, Kind.AFFECTION to (p.needs.affection + bond*.2f), Kind.FOOT_DUET to 45f, Kind.COPY_STRETCH to 35f, Kind.TAG to (if(p.needs.energy>=40 && !weather.raining && weather.outfit!=Outfit.NIGHT) p.needs.mischief else 0f), Kind.PERSONAL to 25f, Kind.KISS to (p.needs.affection*.45f)).filter { now >= (cooldowns[it.first] ?: 0) }
                if(weights.isNotEmpty()) { var pick = random.nextFloat()*weights.sumOf { it.second.toDouble() }.toFloat(); val kind = weights.firstOrNull { pick -= it.second; pick <= 0 }?.first ?: weights.last().first; start(kind,p.who,now) }
            }
        }
    }
    fun startIdle(who: Who, antic: IdleAntic, now: Long): Boolean {
        val p=pet(who)
        if(keyboardOpen || returningFromKeyboard || held!=null || deviceHeat.hot || interaction!=null || p.state!=State.IDLE || !p.motion.grounded) return false
        if(antic.active && (p.needs.energy<40 || weather.raining || weather.outfit==Outfit.NIGHT)) return false
        if(physics.gravity.y<.7f) return false
        val far=(width-petWidth).coerceAtLeast(0f)
        val destination=(p.x+if(p.x<far/2) petWidth*1.6f else -petWidth*1.6f).coerceIn(0f,far)
        if(!idleLife[who.ordinal].start(antic,now,p.x,destination)) return false
        p.state=State.IDLE_ANTIC
        p.facing=if(pet(who.partner()).x<p.x) -1 else 1
        nextChoice=now+antic.duration+random.nextLong(7000,13000)
        return true
    }
    private fun advanceIdles(now: Long,dt: Float) {
        pets.forEach { p ->
            val controller=idleLife[p.who.ordinal]
            val act=controller.performance ?: return@forEach
            if(p.state!=State.IDLE_ANTIC || !p.motion.grounded || held!=null || deviceHeat.hot) {
                controller.cancel(now)
                if(p.state==State.IDLE_ANTIC) p.state=State.IDLE
                return@forEach
            }
            val elapsed=now-act.started
            if(elapsed>=act.antic.duration) {
                controller.finish(now); p.state=State.IDLE
                p.needs.energy+=if(act.antic==IdleAntic.RUN) -1.5f else .5f; p.needs.bound()
            } else if(act.antic==IdleAntic.RUN && elapsed>=350 && elapsed<act.antic.duration-650) {
                val target=if(elapsed<act.antic.duration/2) act.destination else act.origin
                move(p,target,p.y,dt*1.8f)
            }
        }
    }
    private fun move(p: Pet, x: Float, y: Float, dt: Float) {
        val speed = petHeight*.8f * dt
        if(abs(x-p.x)>1) p.facing=if(x>p.x) 1 else -1
        // Walk along the supporting edge; vertical position is otherwise controlled by gravity.
        if(abs(physics.gravity.y)>=abs(physics.gravity.x)) p.x += (x-p.x).coerceIn(-speed,speed)
        else p.y += (y-p.y).coerceIn(-speed,speed)
        clamp(p)
    }
    private fun advance(i: Interaction, now: Long, dt: Float) {
        val a = pet(i.leader); val b = pet(i.leader.partner()); val elapsed = now-i.since
        if(now-i.started > 30000 || (i.kind == Kind.SNACK && prop == null) || (i.kind == Kind.PERSONAL && a.state != State.EXERCISING && prop == null)) { interrupt(now); return }
        fun stage() { i.stage++; i.since = now }
        fun approach(mover: Pet, target: Pet): Boolean {
            mover.state = State.APPROACHING
            val vertical=abs(physics.gravity.x)>abs(physics.gravity.y)
            val desired = if(target.x + petWidth * 1.7f < width) target.x + petWidth*.85f else target.x - petWidth*.85f
            val desiredY=if(target.y+petHeight*1.7f<height) target.y+petHeight*.85f else target.y-petHeight*.85f
            move(mover,desired.coerceIn(0f,(width-petWidth).coerceAtLeast(0f)),if(vertical) desiredY.coerceIn(0f,(height-petHeight).coerceAtLeast(0f)) else target.y,dt)
            return if(vertical) abs(mover.y-target.y)<=petHeight*1.05f && abs(mover.x-target.x)<8f else abs(mover.x-target.x) <= petWidth*1.05f && abs(mover.y-target.y) < 8f
        }
        when(i.kind) {
            Kind.FOOT_DUET, Kind.COPY_STRETCH, Kind.TAG -> when(i.stage) {
                0 -> if(approach(a,b)) { a.state=State.SHARED; stage() }
                1 -> if(elapsed>=800) {
                    a.target=if(a.x<b.x) 0f else width-petWidth
                    stage()
                }
                2 -> {
                    if(i.kind==Kind.TAG) {
                        // A short out-and-back chase; both agree on direction and turn time.
                        val target=if(elapsed<1700) a.target else width-petWidth-a.target
                        move(a,target,a.y,dt*1.8f)
                        if(elapsed>350) move(b,(a.x+if(target<a.x) petWidth*.9f else -petWidth*.9f).coerceIn(0f,width-petWidth),b.y,dt*1.65f)
                    }
                    if(elapsed>=if(i.kind==Kind.TAG) 3400 else 4300) { stage(); if(random.nextFloat()<.35f) say(b.who,if(i.kind==Kind.TAG) "Je t’ai eu !" else "Comme toi ♥",now) }
                }
                else -> if(elapsed>=1000) finish(now)
            }
            Kind.KISS -> when(i.stage) {
                0 -> {
                    a.state=State.APPROACHING
                    val distance=petWidth*.60f
                    val target=(if(a.x<b.x) b.x-distance else b.x+distance).coerceIn(0f,(width-petWidth).coerceAtLeast(0f))
                    move(a,target,b.y,dt)
                    if(abs(a.x-target)<3 && abs(a.y-b.y)<8) { a.state=State.SHARED; stage() }
                }
                1 -> if(elapsed>=650) { stage(); nextHeart=now }
                2 -> if(elapsed>=1500) { say(a.who,"Mwah ♥",now); stage() }
                else -> if(elapsed>=1100) finish(now)
            }
            Kind.SNACK -> when(i.stage) {
                0 -> if(elapsed > 2300) stage()
                1 -> if(approach(b,a)) { b.state = State.SHARED; prop?.owner = b.who; stage() }
                2 -> if(elapsed > 1000) { a.state = State.REACTING; say(a.who,"Hé ! Mon goûter !",now); stage() }
                3 -> { b.state = State.WANDERING; a.state = State.APPROACHING; val away = if(b.x >= a.x) width-petWidth else 0f; move(b,away,b.y,dt); move(a,b.x,a.y,dt); if(elapsed > 2800) { a.state = State.SHARED; b.state = State.SHARED; say(b.who,"Notre goûter ?",now); stage() } }
                4 -> if(elapsed > 3200) { prop?.owner = a.who; a.state = State.EATING; b.state = State.EATING; say(a.who,"Moitié-moitié ? D’accord.",now); stage() }
                else -> if(elapsed > 3000) { a.needs.hunger -= 18; b.needs.hunger -= 18; finish(now) }
            }
            Kind.DINO -> when(i.stage) { 0 -> if(elapsed > 3200) { say(b.who,"rawrrr",now); stage() }; else -> if(elapsed > 3500) finish(now) }
            Kind.AFFECTION -> when(i.stage) { 0 -> if(approach(a,b)) { a.state = State.SHARED; say(b.who,"Te voilà, toi.",now); stage() }; 1 -> if(elapsed > 3300) { say(a.who,"Ma personne préférée.",now); stage() }; else -> if(elapsed > 3500) finish(now) }
            Kind.PERSONAL -> when(i.stage) { 0 -> if(elapsed > 3500) { say(b.who, if(a.who == Who.HUSBAND) "Mon petit génie affamé." else "Mignonne… et redoutable.",now); stage() }; else -> if(elapsed > 4000) finish(now) }
        }
    }
    private fun finish(now: Long) {
        interaction?.let { cooldowns[it.kind] = now + when(it.kind) { Kind.SNACK -> 55000; Kind.DINO -> 120000; else -> 75000 } }
        bond = (bond + 1).coerceIn(0f,100f)
        pets.forEach { it.state = State.IDLE; it.needs.affection += 1; it.needs.bound() }
        interaction = null; prop = null; nextChoice = now + random.nextLong(10000,22000)
    }
    private fun say(who: Who, text: String, now: Long) {
        if(now < nextSpeech || text == lastText) return
        bubble = Bubble(who,text,now+2800); lastText = text; nextSpeech = now+3000
    }
    fun pose(who: Who,now: Long=last): Pose {
        val p = pet(who); val i = interaction
        if(p.state==State.PEEKING) return Pose.PEEK
        if(p.state==State.RETREATING) return Pose.WALK
        if(p.state==State.DRAGGED) return Pose.FALL
        if(p.state==State.PARACHUTING) return Pose.PARACHUTE
        if(i?.kind==Kind.KISS && i.stage>0) return when(i.stage) { 1 -> Pose.KISS_ENTER; 2 -> Pose.KISS; else -> Pose.KISS_AFTER }
        if(now<p.motion.landedUntil) return Pose.LAND
        if(!p.motion.grounded) return Pose.FALL
        idleLife[who.ordinal].performance?.let { if(p.state==State.IDLE_ANTIC) return it.pose(now) }
        if(i?.kind in listOf(Kind.FOOT_DUET,Kind.COPY_STRETCH,Kind.TAG) && i!=null && i.stage>0) {
            if(i.stage==1) return if(who==i.leader) Pose.SIGNATURE else Pose.IDLE
            if(i.stage>=3) return if(who==i.leader) Pose.SMIRK else Pose.WINK
            if(who!=i.leader && now-i.since<if(i.kind==Kind.TAG) 350 else 650) return Pose.IDLE
            return when(i.kind) { Kind.FOOT_DUET -> Pose.FEET; Kind.COPY_STRETCH -> Pose.STRETCH; else -> Pose.RUN }
        }
        if(p.state in listOf(State.RECOVERING,State.REACTING)) return Pose.SURPRISE
        if(p.state in listOf(State.WANDERING,State.APPROACHING)) return Pose.WALK
        if(p.state==State.COOLING) return when(cooling[who.ordinal].phase) {
            CoolingPhase.FANNING -> Pose.FAN
            CoolingPhase.PLACING,CoolingPhase.SWITCHING -> Pose.PLACE_FAN
            CoolingPhase.BREEZE -> Pose.BREEZE
            else -> Pose.REST
        }
        if(p.state == State.EATING) return Pose.EAT
        if(p.state == State.RESTING) return Pose.REST
        if(i?.kind == Kind.DINO && (i.leader == who || i.stage > 0)) return Pose.CLAW
        if(i?.kind == Kind.AFFECTION && i.stage > 0) return Pose.HUG
        if(i?.kind == Kind.SNACK && i.stage==2 && who!=i.leader) return Pose.REACH
        if(i?.kind == Kind.PERSONAL && i.leader == who) return if(p.state==State.FIXING) Pose.FIX else Pose.ANTIC
        if(i?.kind == Kind.PERSONAL && i.stage>0 && i.leader!=who) return Pose.WINK
        if(p.state==State.IDLE && now<p.idleAccentUntil) return if(who==Who.WIFE) Pose.WINK else Pose.SMIRK
        return Pose.IDLE
    }
    fun environment(reading: WeatherReading?,wallTime: Long,hour: Int,now: Long,kindOverride: WeatherKind?=null,tempOverride: Float?=null) {
        environmentKind=kindOverride; environmentTemp=tempOverride
        environmentReading=reading; environmentWall=wallTime; environmentHour=hour
        val changed=weather.update(reading,wallTime,hour,deviceHeat.hot,environmentKind,environmentTemp)
        if(changed && !keyboardOpen && interaction==null && now>=nextWeatherSpeech) {
            nextWeatherSpeech=now+600_000
            say(Who.HUSBAND,weather.line(Who.HUSBAND),now)
        }
    }
    fun batteryTemperature(tenths: Int?,now: Long) {
        val changed=deviceHeat.sample(tenths)
        weather.update(environmentReading,environmentWall,environmentHour,deviceHeat.hot,environmentKind,environmentTemp)
        if(changed) {
            cooling.forEach { it.cancel(now) }
            if(deviceHeat.hot && (interaction!=null || idleLife.any { it.performance!=null })) interrupt(now)
            if(!deviceHeat.hot) pets.filter { it.state==State.COOLING }.forEach { it.state=State.IDLE }
            if(deviceHeat.hot && !keyboardOpen) say(Who.HUSBAND,"Le téléphone chauffe… éventail prêt !",now)
        }
    }
    fun restoreElapsed(seconds: Long) { val hours = seconds.coerceIn(0, 8*3600)/3600f; pets.forEach { it.needs.hunger += hours*3; it.needs.energy += hours*4; it.needs.bound() }; bond = if(bond.isFinite()) bond.coerceIn(0f,100f) else 25f }
    fun resetClock() { last = 0 }
}
