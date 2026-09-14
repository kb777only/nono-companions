package dev.nono.companions

/** Short leases prevent a detached/stalled visual update from retaining its last canopy forever. */
class ArtLease(private val lifetime: Long=500) {
    private var until=Long.MIN_VALUE
    fun renew(now: Long) { until=now+lifetime }
    fun clear() { until=Long.MIN_VALUE }
    fun visible(now: Long)=now<until
}

fun canopyVisible(p: Pet,physics: GravityPhysics,w: Float,h: Float,pw: Float,ph: Float)=
    p.state==State.PARACHUTING && p.motion.fallSince>=0 && !physics.supported(p,w,h,pw,ph)

fun rainFrame(frame: Int): Int=when(frame) {
    44,45 -> 5
    46 -> 11
    47 -> 9
    else -> dressedFrame(frame)
}
fun umbrellaPose(raining: Boolean,frame: Int)=raining && frame !in 8..11 && frame !in 40..43
