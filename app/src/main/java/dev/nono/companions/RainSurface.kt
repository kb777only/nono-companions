package dev.nono.companions

/** Umbrella upper silhouette in the same character-local coordinates used for drawing. */
data class RainSurface(val left: Float,val top: Float,val width: Float,val height: Float,val profile: FloatArray,val mirrored: Boolean=false) {
    fun at(x: Float): Float? {
        val local=(if(mirrored) 1-x else x)-left
        val u=local/width
        if(u !in 0f..1f || profile.size<2) return null
        val position=u*(profile.size-1)
        val i=position.toInt().coerceAtMost(profile.size-2)
        val a=profile[i]; val b=profile[i+1]
        if(!a.isFinite() || !b.isFinite()) return null
        return top+(a+(b-a)*(position-i))*height
    }
}
