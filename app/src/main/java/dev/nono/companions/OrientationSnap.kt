package dev.nono.companions

import kotlin.math.*

/** Hysteresis and dwell reject boundary jitter; eased shortest-path turns never spin a full circle. */
class OrientationSnap {
    var target=0f; private set
    private var candidate=0f
    private var candidateAt=0L
    private var from=0f
    private var started=0L
    private var duration=0L
    fun sample(gravity: GravityVector, reliable: Boolean, now: Long): Boolean {
        if(!reliable || !gravity.x.isFinite() || !gravity.y.isFinite()) { candidate=target; candidateAt=now; return false }
        val degrees=Math.toDegrees(atan2(-gravity.x,gravity.y).toDouble()).toFloat()
        if(abs(delta(target,degrees))<55f) { candidate=target; candidateAt=now; return false }
        val next=normalize(round(degrees/90f)*90f)
        if(next!=candidate) { candidate=next; candidateAt=now; return false }
        if(next==target || now-candidateAt<250) return false
        from=value(now); target=next; started=now
        duration=if(abs(delta(from,target))>100) 650 else 450
        return true
    }
    fun value(now: Long): Float {
        if(duration==0L) return target
        val t=((now-started).toFloat()/duration).coerceIn(0f,1f)
        val smooth=t*t*(3-2*t)
        return normalize(from+delta(from,target)*smooth)
    }
    fun moving(now: Long)=now-started<duration
    companion object {
        fun normalize(angle: Float)=((angle+180f)%360f+360f)%360f-180f
        fun delta(from: Float,to: Float)=normalize(to-from)
        fun extent(width: Float,height: Float,angle: Float): Pair<Float,Float> {
            val r=Math.toRadians(angle.toDouble()); val c=abs(cos(r)).toFloat(); val s=abs(sin(r)).toFloat()
            return ceil(width*c+height*s) to ceil(width*s+height*c)
        }
    }
}

/** Window metadata takes precedence when available; no text or input contents are involved. */
class KeyboardSignals {
    var insetBottom=0
    var windowTop: Int?=null
    var windowVisible: Boolean?=null
    fun bottom(displayHeight: Int): Int = when(windowVisible) {
        true -> (displayHeight-(windowTop ?: displayHeight)).coerceIn(0,displayHeight)
        false -> 0
        null -> insetBottom.coerceIn(0,displayHeight)
    }
}
