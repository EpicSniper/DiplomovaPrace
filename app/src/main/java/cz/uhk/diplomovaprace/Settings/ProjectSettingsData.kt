package cz.uhk.diplomovaprace.Settings

data class ProjectSettingsData(
    val algorithmType: String?,
    val pitchOfA1: Int?,
    val bpm: Int?,
    val timeSignatureNumerator: Int?,
    val timeSignatureDenominator: Int?
)