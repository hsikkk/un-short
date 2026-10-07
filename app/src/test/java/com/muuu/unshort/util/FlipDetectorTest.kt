package com.muuu.unshort.util

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class FlipDetectorTest {
    private val detector = FlipDetector(RuntimeEnvironment.getApplication())
    private val samples = mutableListOf<Boolean>()
    private val listener = object : FlipDetector.FlipListener {
        override fun onFlipDetected(isFlipped: Boolean) { samples.add(isFlipped) }
    }

    private fun sample(z: Float) {
        val event = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.newInstance(3)
        event.sensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        event.values[2] = z
        detector.onSensorChanged(event)
    }

    @Test fun smallMovementDoesNotPauseButLiftingPhoneDoes() {
        detector.start(listener)
        sample(-9.8f)
        sample(-7.9f)
        sample(-8.1f)
        sample(0f)
        assertEquals(listOf(true, false), samples)
        detector.stop()
    }

    @Test fun resumeReportsCurrentPoseWithoutRequiringAnotherFlip() {
        detector.start(listener)
        sample(-9.8f)
        detector.stop()
        detector.start(listener)
        sample(-9.8f)
        assertEquals(listOf(true, true), samples)
        detector.stop()
    }
}
