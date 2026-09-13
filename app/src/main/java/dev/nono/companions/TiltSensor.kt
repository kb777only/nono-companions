package dev.nono.companions

import android.content.Context
import android.hardware.*
import android.view.Surface

/** Gravity is fused by Android (normally accelerometer + gyro), avoiding integrated gyro drift. */
class TiltSensor(context: Context, private val rotation: () -> Int, private val changed: (GravityVector, Boolean) -> Unit) : SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java)
    private val sensor=manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: manager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val matrix=FloatArray(9)
    private var started=false
    private var x=0f; private var y=0f; private var initialized=false
    fun start() { if(!started && sensor!=null) { initialized=false; started=manager.registerListener(this,sensor,100_000) } }
    fun stop() { manager.unregisterListener(this); started=false }
    override fun onSensorChanged(event: SensorEvent) {
        val fused=sensor?.type==Sensor.TYPE_GAME_ROTATION_VECTOR
        if(fused) SensorManager.getRotationMatrixFromVector(matrix,event.values)
        val sx=if(fused) matrix[6]*9.81f else event.values[0]
        val sy=if(fused) matrix[7]*9.81f else event.values[1]
        if(!initialized) { x=sx; y=sy; initialized=true }
        val blend=if(fused || sensor?.type==Sensor.TYPE_GRAVITY) .4f else .15f
        x+=(sx-x)*blend; y+=(sy-y)*blend
        val quarterTurns=when(rotation()) { Surface.ROTATION_90 -> 1; Surface.ROTATION_180 -> 2; Surface.ROTATION_270 -> 3; else -> 0 }
        changed(GravityMapping.screen(x,y,quarterTurns),kotlin.math.hypot(x,y)>=2.2f)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
