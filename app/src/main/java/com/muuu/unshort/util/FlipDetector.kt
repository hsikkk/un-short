package com.muuu.unshort.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.muuu.unshort.util.FlipDetector

class FlipDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isFlipped: Boolean? = null
    private var listener: FlipListener? = null

    interface FlipListener {
        fun onFlipDetected(isFlipped: Boolean)
    }

    fun start(listener: FlipListener) {
        sensorManager.unregisterListener(this)
        this.listener = listener
        isFlipped = null // 첫 샘플로 현재 자세를 다시 전달한다.
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (!registered) Log.w("FlipDetector", "Accelerometer listener registration failed")
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
            // 진입/이탈 임계치를 분리해 경계에서의 작은 흔들림으로 멈추지 않게 한다.
            isFlipped = if (wasFlipped == true) z < -6.5f else z < -8.0f

            // 상태가 변경되었을 때만 알림
            if (wasFlipped != isFlipped) {
                listener?.onFlipDetected(isFlipped == true)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }
}
