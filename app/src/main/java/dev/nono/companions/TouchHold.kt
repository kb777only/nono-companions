package dev.nono.companions

/** Movement always wins, even after the long-press menu has appeared. */
class TouchHold(private val slop: Float, private val delay: Long) {
    var dragging=false; private set
    var held=false; private set
    private var active=false
    private var since=0L
    fun down(now: Long) { active=true; since=now; dragging=false; held=false }
    fun move(dx: Float,dy: Float): Boolean {
        if(active && dx*dx+dy*dy>slop*slop) dragging=true
        return dragging
    }
    fun hold(now: Long): Boolean {
        if(!active || dragging || held || now-since<delay) return false
        held=true; return true
    }
    fun end(): Boolean { val tap=active && !dragging && !held; active=false; return tap }
}

data class KissHeart(val who: Who,val born: Long,val life: Long,val x: Float,val y: Float) {
    fun progress(now: Long)=((now-born).toFloat()/life).coerceIn(0f,1f)
}
