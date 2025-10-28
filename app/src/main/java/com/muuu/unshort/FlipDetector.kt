package com.muuu.unshort

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class FlipDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isFlipped = false
    private var listener: FlipListener? = null

    interface FlipListener {
        fun onFlipDetected(isFlipped: Boolean)
    }

    fun start(listener: FlipListener) {
        this.listener = listener
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]

            // Z축 가속도가 -9.8 근처면 폰이 뒤집어진 상태
            // (화면이 아래를 향함)
            val wasFlipped = isFlipped
            isFlipped = z < -8.0f

            // 상태가 변경되었을 때만 알림
            if (wasFlipped != isFlipped) {
                listener?.onFlipDetected(isFlipped)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }

    fun isCurrentlyFlipped(): Boolean = isFlipped
}
