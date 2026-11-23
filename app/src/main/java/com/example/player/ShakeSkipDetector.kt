package com.example.player

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Lightweight accelerometer-based Shake-to-Skip detector using standard Android SensorManager
 * (no permissions required).
 */
class ShakeSkipDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isRegistered = false
    private var lastTriggerAtMs = 0L

    fun start() {
        if (isRegistered || sensorManager == null || accelerometer == null) return
        isRegistered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        if (!isRegistered || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values.size < 3) return
        val now = System.currentTimeMillis()
        if (evaluateShakeTrigger(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                nowMs = now,
                lastTriggerMs = lastTriggerAtMs
            )
        ) {
            lastTriggerAtMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.35f
        private const val COOLDOWN_MS = 1_600L

        /**
         * Computes gravity-normalized G-force from raw accelerometer axes and checks cooldown.
         */
        fun evaluateShakeTrigger(
            x: Float,
            y: Float,
            z: Float,
            nowMs: Long,
            lastTriggerMs: Long,
            thresholdG: Float = SHAKE_THRESHOLD_G,
            cooldownMs: Long = COOLDOWN_MS
        ): Boolean {
            if (nowMs - lastTriggerMs < cooldownMs) return false
            val gx = x / SensorManager.GRAVITY_EARTH
            val gy = y / SensorManager.GRAVITY_EARTH
            val gz = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gx * gx + gy * gy + gz * gz)
            return gForce >= thresholdG
        }
    }
}
