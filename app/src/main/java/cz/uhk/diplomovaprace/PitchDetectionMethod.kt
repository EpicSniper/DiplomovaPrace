package cz.uhk.diplomovaprace

enum class PitchDetectionMethod(val method: String) {
    AUTOCORRELATION("Auto-correlation"),
    HPS("Harmonic Product Spectrum"),
}