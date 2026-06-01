package com.example.dsp

import kotlin.math.*

class BiquadFilter {
    // Coefficients
    private var b0: Double = 1.0
    private var b1: Double = 0.0
    private var b2: Double = 0.0
    private var a1: Double = 0.0
    private var a2: Double = 0.0

    // State history
    private var x1: Double = 0.0
    private var x2: Double = 0.0
    private var y1: Double = 0.0
    private var y2: Double = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        
        // Anti-denormal check to prevent CPU spikes in floating point
        val out = if (y.isNaN() || y.isInfinite() || abs(y) < 1e-25) 0.0 else y
        
        x2 = x1
        x1 = x
        y2 = y1
        y1 = out
        
        return out
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    fun setPeakingEq(sampleRate: Double, centerFreq: Double, q: Double, dbGain: Double) {
        val w0 = 2.0 * PI * centerFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val a = 10.0.pow(dbGain / 40.0)

        val cosW0 = cos(w0)
        val b0Temp = 1.0 + alpha * a
        val b1Temp = -2.0 * cosW0
        val b2Temp = 1.0 - alpha * a
        val a0Temp = 1.0 + alpha / a
        val a1Temp = -2.0 * cosW0
        val a2Temp = 1.0 - alpha / a

        b0 = b0Temp / a0Temp
        b1 = b1Temp / a0Temp
        b2 = b2Temp / a0Temp
        a1 = a1Temp / a0Temp
        a2 = a2Temp / a0Temp
    }

    fun setLowPass(sampleRate: Double, cutoffFreq: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * cutoffFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val b0Temp = (1.0 - cosW0) / 2.0
        val b1Temp = 1.0 - cosW0
        val b2Temp = (1.0 - cosW0) / 2.0
        val a0Temp = 1.0 + alpha
        val a1Temp = -2.0 * cosW0
        val a2Temp = 1.0 - alpha

        b0 = b0Temp / a0Temp
        b1 = b1Temp / a0Temp
        b2 = b2Temp / a0Temp
        a1 = a1Temp / a0Temp
        a2 = a2Temp / a0Temp
    }

    fun setHighPass(sampleRate: Double, cutoffFreq: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * cutoffFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val b0Temp = (1.0 + cosW0) / 2.0
        val b1Temp = -(1.0 + cosW0)
        val b2Temp = (1.0 + cosW0) / 2.0
        val a0Temp = 1.0 + alpha
        val a1Temp = -2.0 * cosW0
        val a2Temp = 1.0 - alpha

        b0 = b0Temp / a0Temp
        b1 = b1Temp / a0Temp
        b2 = b2Temp / a0Temp
        a1 = a1Temp / a0Temp
        a2 = a2Temp / a0Temp
    }

    fun setBandPass(sampleRate: Double, centerFreq: Double, q: Double) {
        val w0 = 2.0 * PI * centerFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val b0Temp = alpha
        val b1Temp = 0.0
        val b2Temp = -alpha
        val a0Temp = 1.0 + alpha
        val a1Temp = -2.0 * cosW0
        val a2Temp = 1.0 - alpha

        b0 = b0Temp / a0Temp
        b1 = b1Temp / a0Temp
        b2 = b2Temp / a0Temp
        a1 = a1Temp / a0Temp
        a2 = a2Temp / a0Temp
    }
}
