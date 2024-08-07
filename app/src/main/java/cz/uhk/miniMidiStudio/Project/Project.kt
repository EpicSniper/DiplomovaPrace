package cz.uhk.miniMidiStudio.Project

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
class Project {
    private var tracks = ArrayList<Track>()
    private var name: String = "New project"
    private var tempo: Int = 120
    private var timeSignatureUpper: Int = 4
    private var timeSignatureLower: Int = 4
    private var createdAt: String = LocalDateTime.now().toString()
    private var uuid: String = ""
    private var algorithmType: String = "default"
    private var pitchOfA1: Int = 440

    fun getPitchOfA1(): Int {
        return pitchOfA1
    }

    fun setPitchOfA1(newPitchOfA1: Int?) {
        if (newPitchOfA1 != null) {
            pitchOfA1 = newPitchOfA1
        }
    }

    fun getAlgorithmType(): String {
        return algorithmType
    }

    fun setAlgorithmType(newAlgorithmType: String?) {
        if (newAlgorithmType != null) {
            algorithmType = newAlgorithmType
        }
    }

    fun getUuid(): String {
        return uuid
    }

    fun setUuid(newUuid: String) {
        uuid = newUuid
    }

    fun getCreatedAt(): String {
        return createdAt
    }

    fun getFormattedCreatedAt(): String {
        return createdAt
    }

    fun setCreatedAt(newCreatedAt: LocalDateTime) {
        createdAt = newCreatedAt.toString()
    }

    fun getTracks(): ArrayList<Track> {
        return tracks
    }

    fun setTracks(newTracks: ArrayList<Track>) {
        tracks = newTracks
    }

    fun addTrack(track: Track) {
        tracks.add(track)
    }

    fun removeTrack(track: Track) {
        tracks.remove(track)
    }

    fun getName(): String {
        return name
    }

    fun setName(newName: String) {
        name = newName
    }

    fun getTempo(): Int {
        return tempo
    }

    fun setTempo(newTempo: Int?) {
        if (newTempo != null) {
            tempo = newTempo
        }
    }

    fun getTimeSignatureUpper(): Int {
        return timeSignatureUpper
    }

    fun setTimeSignatureUpper(newTimeSignatureUpper: Int?) {
        if (newTimeSignatureUpper != null) {
            timeSignatureUpper = newTimeSignatureUpper
        }
    }

    fun getTimeSignatureLower(): Int {
        return timeSignatureLower
    }

    fun setTimeSignatureLower(newTimeSignatureLower: Int?) {
        if (newTimeSignatureLower != null) {
            timeSignatureLower = newTimeSignatureLower
        }
    }
}