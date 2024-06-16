package cz.uhk.diplomovaprace.PianoRoll.Midi

import java.time.LocalDateTime

class Project {
    private var tracks = ArrayList<Track>()
    private var name: String = ""
    private var tempo: Int = 120
    private var timeSignatureUpper: Int = 4
    private var timeSignatureLower: Int = 4
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun getCreatedAt(): LocalDateTime {
        return createdAt
    }

    fun getFormattedCreatedAt(): String {
        return createdAt.toString()
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
}