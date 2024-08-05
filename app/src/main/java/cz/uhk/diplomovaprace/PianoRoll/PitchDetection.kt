package cz.uhk.diplomovaprace.PianoRoll

import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.sqrt

class PitchDetection {
    // private fun getPitchPWPD(audioData: ShortArray, sampleRate: Int): Double

    // private fun getPitchMLE(audioData: ShortArray, sampleRate: Int): Double
    public fun getAutocorrelationPitch(audioData: ShortArray, sampleRate: Int): Double {

        // autocorelation method
        val numSamples = audioData.size
        val audioDataDouble = DoubleArray(numSamples)

        // Convert audio samples to array of doubles between -1 and 1
        for (i in 0 until numSamples) {
            audioDataDouble[i] =
                audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
        }

        val minPeriod =
            (sampleRate / 2000) // Minimum period for pitch detection (e.g., 1000 Hz)
        val maxPeriod = (sampleRate / 80) // Maximum period for pitch detection (e.g., 200 Hz)

        var pitchPeriod = 0
        var maxCorrelation = 0.0

        // Calculate autocorrelation for different pitch periods
        for (period in minPeriod until maxPeriod) {
            var correlation = 0.0
            for (i in 0 until numSamples - period) {
                correlation += audioDataDouble[i] * audioDataDouble[i + period]
            }

            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                pitchPeriod = period
            }
        }

        // Calculate pitch in Hz
        return sampleRate.toDouble() / pitchPeriod.toDouble()
    }

    public fun getHPSPitch(audioData: ShortArray, sampleRate: Int): Double {
        val numSamples = audioData.size
        val audioDataDouble = DoubleArray(numSamples)

        // Convert audio samples to array of doubles between -1 and 1
        for (i in 0 until numSamples) {
            audioDataDouble[i] =
                audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
        }

        // Apply FFT to get frequency spectrum
        val fft = DoubleFFT_1D(numSamples.toLong())
        fft.realForward(audioDataDouble)

        // Calculate magnitude spectrum
        val magnitudeSpectrum = DoubleArray(numSamples / 2)
        for (i in 0 until numSamples / 2) {
            val real = audioDataDouble[2 * i]
            val imag = audioDataDouble[2 * i + 1]
            magnitudeSpectrum[i] = sqrt(real * real + imag * imag)
        }

        // Apply Harmonic Product Spectrum method
        val maxHarmonics = 5
        val hps = DoubleArray(numSamples / (2 * maxHarmonics))
        for (i in hps.indices) {
            hps[i] = magnitudeSpectrum[i]
            for (j in 2..maxHarmonics) {
                if (i * j < magnitudeSpectrum.size) {
                    hps[i] *= magnitudeSpectrum[i * j]
                } else {
                    hps[i] = 0.0
                    break
                }
            }
        }

        // Find the peak in the HPS array
        val maxHps = hps.maxOrNull() ?: 0.0
        val maxHpsIndex = hps.toList().indexOf(maxHps)

        // Calculate pitch in Hz
        return sampleRate.toDouble() * maxHpsIndex / numSamples
    }
}