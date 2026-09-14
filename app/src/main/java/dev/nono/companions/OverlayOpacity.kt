package dev.nono.companions

import kotlin.math.*

/** Budget even the worst case where every non-touchable surface overlaps. */
object OverlayOpacity {
    fun distribute(desired: List<Float>): List<Float> {
        require(desired.all { it in 0f.. .99f })
        val total=desired.sumOf { ln((1-it).toDouble()) }
        val exponent=if(total<ln(.22)) ln(.22)/total else 1.0
        return desired.map { (1-(1-it).toDouble().pow(exponent)).toFloat() }
    }
}
