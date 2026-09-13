package dev.nono.companions

import android.content.Context
import android.hardware.*
import android.view.Surface

/** Gravity is fused by Android (normally accelerometer + gyro), avoiding integrated gyro drift. */
class TiltSensor(context: Context, private val rotation: () -> Int, private val changed: (GravityVector) -> Unit) : SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java)
    private val sensor=manager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var started=false
    private var x=0f; private var y=0f; private var initialized=false
    fun start() { if(!started && sensor!=null) { initialized=false; started=manager.registerListener(this,sensor,100_000) } }
    fun stop() { manager.unregisterListener(this); started=false }
    override fun onSensorChanged(event: SensorEvent) {
        if(!initialized) { x=event.values[0]; y=event.values[1]; initialized=true }
        val blend=if(sensor?.type==Sensor.TYPE_GRAVITY) .4f else .15f
        x+=(event.values[0]-x)*blend; y+=(event.values[1]-y)*blend
        val quarterTurns=when(rotation()) { Surface.ROTATION_90 -> 1; Surface.ROTATION_180 -> 2; Surface.ROTATION_270 -> 3; else -> 0 }
        changed(GravityMapping.screen(x,y,quarterTurns))
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
