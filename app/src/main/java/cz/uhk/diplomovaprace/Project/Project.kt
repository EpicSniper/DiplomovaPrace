package cz.uhk.diplomovaprace.Project

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

    fun setTempo(newTempo: Int) {
        tempo = newTempo
    }

    fun getTimeSignatureUpper(): Int {
        return timeSignatureUpper
    }

    fun setTimeSignatureUpper(newTimeSignatureUpper: Int) {
        timeSignatureUpper = newTimeSignatureUpper
    }

    fun getTimeSignatureLower(): Int {
        return timeSignatureLower
    }

    fun setTimeSignatureLower(newTimeSignatureLower: Int) {
        timeSignatureLower = newTimeSignatureLower
    }
}