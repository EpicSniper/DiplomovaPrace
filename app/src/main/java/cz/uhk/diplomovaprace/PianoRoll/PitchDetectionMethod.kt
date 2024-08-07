package cz.uhk.diplomovaprace.PianoRoll

enum class PitchDetectionMethod(val method: String) {
    AUTOCORRELATION("Auto-correlation"),
    HPS("Harmonic Product Spectrum"),
}