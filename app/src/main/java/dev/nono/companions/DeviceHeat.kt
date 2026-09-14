package dev.nono.companions

/** ACTION_BATTERY_CHANGED reports battery temperature in tenths of a degree Celsius.
 * This is deliberately not labelled CPU/skin temperature. No privileged sensors. */
class DeviceHeat {
    var celsius: Float? = null; private set
    var hot = false; private set
    fun sample(tenths: Int?): Boolean {
        val before = hot
        celsius = tenths?.takeIf { it in -400..1000 }?.div(10f)
        hot = celsius?.let { it > 43f || (hot && it > 42f) } ?: false
        return before != hot
    }
}

enum class CoolingPhase { NONE, FANNING, PLACING, SWITCHING, BREEZE, QUIET }

/** Explicit prop lifecycle: hand fan, place desk fan, switch on, enjoy, put away.
 * Eligibility comes from the world, so touch, physics and shared actions win. */
class CoolingController {
    var phase = CoolingPhase.NONE; private set
    private var entered = 0L
    private var retryAt = 0L
    fun elapsed(now: Long)=(now-entered).coerceAtLeast(0L)
    val deskVisible get() = phase in listOf(CoolingPhase.PLACING, CoolingPhase.SWITCHING, CoolingPhase.BREEZE)
    val spinning get() = phase == CoolingPhase.BREEZE
    fun cancel(now: Long) { phase = CoolingPhase.NONE; retryAt = now + 3000 }
    fun tick(hot: Boolean, eligible: Boolean, now: Long) {
        if (!hot || !eligible) { if (phase != CoolingPhase.NONE) cancel(now); return }
        if (phase == CoolingPhase.NONE) {
            if (now < retryAt) return
            phase = CoolingPhase.FANNING; entered = now
        }
        val duration = when (phase) {
            CoolingPhase.FANNING -> 6000L
            CoolingPhase.PLACING -> 900L
            CoolingPhase.SWITCHING -> 700L
            CoolingPhase.BREEZE -> 9000L
            CoolingPhase.QUIET -> 7000L
            CoolingPhase.NONE -> Long.MAX_VALUE
        }
        if (now - entered >= duration) {
            phase = when (phase) {
                CoolingPhase.FANNING -> CoolingPhase.PLACING
                CoolingPhase.PLACING -> CoolingPhase.SWITCHING
                CoolingPhase.SWITCHING -> CoolingPhase.BREEZE
                CoolingPhase.BREEZE -> CoolingPhase.QUIET
                else -> CoolingPhase.FANNING
            }
            entered = now
        }
    }
}
