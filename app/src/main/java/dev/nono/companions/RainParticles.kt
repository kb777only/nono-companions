package dev.nono.companions

import kotlin.math.*
import kotlin.random.Random

data class WaterDrop(var x: Float,var y: Float,var vx: Float,var vy: Float,val born: Long,val life: Long,var bounced: Boolean=false,val trail: Boolean=false) {
    fun alpha(now: Long): Float = minOf(((now-born)/120f).coerceIn(0f,1f),((born+life-now)/250f).coerceIn(0f,1f))
}
/** Character-local coordinates, translated against movement so foot droplets remain behind. */
class RainParticles(private val random: Random=Random.Default) {
    val drops=mutableListOf<WaterDrop>()
    private var last=0L; private var spawn=0L; private var step=0L
    fun clear() { drops.clear(); last=0; spawn=0; step=0 }
    fun tick(now: Long,umbrella: Boolean,walking: Boolean,dx: Float=0f,dy: Float=0f) {
        val dt=if(last==0L) 0f else ((now-last).coerceIn(0,100)/1000f); last=now
        drops.forEach { it.x-=dx; it.y-=dy }
        drops.removeAll { now-it.born>=it.life || it.x !in -1f..2f || it.y>1.45f }
        if(now>=spawn && drops.size<56) {
            repeat(2) { drops.add(WaterDrop(random.nextFloat()*1.5f-.25f,-.70f+random.nextFloat()*.12f,.02f,1.1f+random.nextFloat()*.4f,now,random.nextLong(1300,1900))) }
            spawn=now+90
        }
        if(walking && abs(dx)+abs(dy)>.001f && now>=step && drops.size<60) {
            repeat(minOf(2,60-drops.size)) { drops.add(WaterDrop(.35f+random.nextFloat()*.3f,.96f,(random.nextFloat()-.5f)*.3f,-.18f,now,random.nextLong(450,850),true,true)) }; step=now+150
        }
        drops.forEach { d ->
            val oldY=d.y
            d.x+=d.vx*dt; d.y+=d.vy*dt
            if(d.bounced) d.vy+=.8f*dt
            else {
                val surface=surface(d.x,umbrella)
                if(surface!=null && oldY<=surface && d.y>=surface) {
                    d.y=surface; d.vy=-.25f-random.nextFloat()*.25f
                    d.vx=if(d.x<.5f) -.5f else .5f; d.bounced=true
                }
            }
        }
    }
    fun surface(x: Float,umbrella: Boolean): Float? {
        val dx=x-.5f
        if(umbrella && abs(dx)<.75f) return -.46f+.40f*(dx/.75f).pow(2)
        if(abs(dx)<.32f) return .23f-.21f*sqrt(1-(dx/.32f).pow(2))
        return if(x in .2f.. .8f) .55f else null
    }
}
