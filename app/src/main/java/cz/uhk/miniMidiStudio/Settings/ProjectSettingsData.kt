package cz.uhk.miniMidiStudio.Settings

import kotlinx.serialization.Serializable

@Serializable
data class ProjectSettingsData(
    val algorithmType: String?,
    val pitchOfA1: Int?,
    val bpm: Int?,
    val timeSignatureNumerator: Int?,
    val timeSignatureDenominator: Int?,
    val projectName: String? = "New project"
)