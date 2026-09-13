package dev.nono.companions

import kotlin.math.*

data class Motion(var vx: Float=0f, var vy: Float=0f, var grounded: Boolean=false, var landedUntil: Long=0, var fallSince: Long=-1)
data class GravityVector(val x: Float, val y: Float)

object GravityMapping {
    /** Android sensor axes stay in natural-device orientation. Screen Y points down. */
    fun screen(sensorX: Float, sensorY: Float, quarterTurns: Int): GravityVector {
        val (sx,sy)=when(quarterTurns) { 1 -> -sensorY to sensorX; 2 -> -sensorX to -sensorY; 3 -> sensorY to -sensorX; else -> sensorX to sensorY }
        val x=-sx; val y=sy; val length=hypot(x,y)
        // Flat on a table has essentially no screen-plane gravity; choose conventional down.
        if(!length.isFinite() || length<2.2f) return GravityVector(0f,1f)
        return GravityVector(x/length,y/length)
    }
}

/** Bounded semi-implicit integration with small substeps, wall contact and gentle restitution. */
class GravityPhysics {
    var gravity=GravityVector(0f,1f); private set
    fun setGravity(x: Float,y: Float): Boolean {
        val len=hypot(x,y)
        if(!len.isFinite() || len<.1f) return false
        val next=GravityVector(x/len,y/len)
        val changed=gravity.x*next.x+gravity.y*next.y < .90f
        gravity=next
        return changed
    }
    fun supported(p: Pet,w: Float,h: Float,pw: Float,ph: Float): Boolean {
        val maxX=(w-pw).coerceAtLeast(0f); val maxY=(h-ph).coerceAtLeast(0f)
        return (gravity.y>.35 && p.y>=maxY-.5f) || (gravity.y<-.35 && p.y<=.5f) || (gravity.x>.35 && p.x>=maxX-.5f) || (gravity.x<-.35 && p.x<=.5f)
    }
    fun step(p: Pet,w: Float,h: Float,pw: Float,ph: Float,seconds: Float,now: Long) {
        val m=p.motion
        if(p.state==State.DRAGGED) { m.vx=0f; m.vy=0f; m.grounded=false; m.fallSince=-1; return }
        if(!supported(p,w,h,pw,ph)) {
            if(m.fallSince<0) m.fallSince=now
            p.state=if(now-m.fallSince>=650) State.PARACHUTING else State.FALLING
        }
        val gliding=p.state==State.PARACHUTING
        val maxX=(w-pw).coerceAtLeast(0f); val maxY=(h-ph).coerceAtLeast(0f)
        val acceleration=ph*8f
        var left=seconds.coerceIn(0f,1f)
        var hitSpeed=0f
        val wasGrounded=m.grounded
        while(left>0f) {
            val dt=min(left,1f/60f); left-=dt
            val contact=supported(p,w,h,pw,ph)
            val onY=(gravity.y>.35 && p.y>=maxY-.5f)||(gravity.y<-.35 && p.y<=.5f)
            val onX=(gravity.x>.35 && p.x>=maxX-.5f)||(gravity.x<-.35 && p.x<=.5f)
            val gx=if(onY && abs(gravity.x)<.35f) 0f else gravity.x
            val gy=if(onX && abs(gravity.y)<.35f) 0f else gravity.y
            m.vx=(m.vx+gx*acceleration*dt).coerceIn(-ph*10,ph*10)
            m.vy=(m.vy+gy*acceleration*dt).coerceIn(-ph*10,ph*10)
            if(contact) { if(onY) m.vx*=exp(-8f*dt); if(onX) m.vy*=exp(-8f*dt) }
            if(gliding) {
                // Cap descent along current screen-plane gravity; tilt still changes the glide direction.
                val along=m.vx*gravity.x+m.vy*gravity.y
                val capped=along.coerceAtMost(ph*.85f)
                val sideways=(m.vx*(-gravity.y)+m.vy*gravity.x)*exp(-3f*dt)
                m.vx=gravity.x*capped-gravity.y*sideways
                m.vy=gravity.y*capped+gravity.x*sideways
            }
            p.x+=m.vx*dt; p.y+=m.vy*dt
            if(p.x<0 || p.x>maxX) { hitSpeed=max(hitSpeed,abs(m.vx)); p.x=p.x.coerceIn(0f,maxX); m.vx=if(abs(m.vx)>ph*2) -m.vx*.12f else 0f }
            if(p.y<0 || p.y>maxY) { hitSpeed=max(hitSpeed,abs(m.vy)); p.y=p.y.coerceIn(0f,maxY); m.vy=if(abs(m.vy)>ph*2) -m.vy*.12f else 0f }
        }
        m.grounded=supported(p,w,h,pw,ph) && abs(m.vx)+abs(m.vy)<ph*.6f
        if(m.grounded) {
            m.fallSince=-1
            if(p.state==State.FALLING || p.state==State.PARACHUTING) {
                p.state=State.RECOVERING; p.until=now+500; m.landedUntil=now+500
            }
        }
        if(!wasGrounded && hitSpeed>ph*1.1f) m.landedUntil=now+500
    }
}
