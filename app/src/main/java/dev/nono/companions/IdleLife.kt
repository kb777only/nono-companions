package dev.nono.companions

import kotlin.random.Random

/** The same four clip families have separately drawn, character-specific performances. */
enum class IdleAntic(val pose: Pose, val duration: Long, val active: Boolean) {
    FEET(Pose.FEET, 5600, false), STRETCH(Pose.STRETCH, 5100, true),
    SIGNATURE(Pose.SIGNATURE, 4400, false), RUN(Pose.RUN, 4200, true);

    fun label(who: Who) = when(this) {
        FEET -> if(who==Who.HUSBAND) "Pieds impatients, ventre affamé" else "Petits coups de bottes"
        STRETCH -> if(who==Who.HUSBAND) "Échauffement sportif" else "Étirement tout doux"
        SIGNATURE -> if(who==Who.HUSBAND) "Lunettes et idée de génie" else "Mèche, coucou et clin d’œil"
        RUN -> if(who==Who.HUSBAND) "Mission goûter au pas de course" else "Course de petite chipie"
    }
}

data class IdlePerformance(val antic: IdleAntic, val started: Long, val origin: Float, val destination: Float) {
    fun pose(now: Long): Pose {
        val elapsed=now-started
        return when {
            elapsed<350 -> Pose.IDLE
            elapsed>=antic.duration-650 -> Pose.SMIRK
            else -> antic.pose
        }
    }
}

/** Per-character cooldowns survive interruptions; performances do not survive process death. */
class IdleLife {
    var performance: IdlePerformance?=null; private set
    var readyAt=0L; private set
    private val cooldowns=mutableMapOf<IdleAntic,Long>()
    fun choose(needs: Needs, quiet: Boolean, now: Long, random: Random): IdleAntic? {
        val choices=IdleAntic.entries.filter { now >= (cooldowns[it] ?: 0) && (!it.active || (!quiet && needs.energy>=40)) }
        val weighted=choices.map { it to when(it) {
            IdleAntic.FEET -> 30f+(100-needs.energy)*.4f+needs.hunger*.15f
            IdleAntic.STRETCH -> 30f
            IdleAntic.SIGNATURE -> 30f+needs.affection*.35f
            IdleAntic.RUN -> needs.energy*.35f+needs.mischief*.35f
        } }
        var pick=random.nextFloat()*weighted.sumOf { it.second.toDouble() }.toFloat()
        return weighted.firstOrNull { pick-=it.second; pick<=0 }?.first ?: weighted.lastOrNull()?.first
    }
    fun start(antic: IdleAntic, now: Long, origin: Float, destination: Float): Boolean {
        if(performance!=null || now<readyAt || now<(cooldowns[antic] ?: 0)) return false
        performance=IdlePerformance(antic,now,origin,destination)
        cooldowns[antic]=now+when(antic) { IdleAntic.RUN -> 95000; IdleAntic.STRETCH -> 75000; else -> 55000 }
        return true
    }
    fun cancel(now: Long) {
        if(performance!=null) readyAt=maxOf(readyAt,now+8000)
        performance=null
    }
    fun finish(now: Long) { performance=null; readyAt=now+12000 }
}
