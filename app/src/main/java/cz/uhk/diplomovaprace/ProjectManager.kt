package cz.uhk.diplomovaprace

import android.content.Context
import cz.uhk.diplomovaprace.PianoRoll.Midi.Project
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ProjectManager {

    private var projectsDirectory: String = "projects"

    public fun saveProjectToFile(project: Project, context: Context) {
        // Save the track to a file

        val filename = project.getName() + UUID.randomUUID().toString().replace("-", "")
        val serialized = Json.encodeToString(project)


        val directory = context.getDir(projectsDirectory, Context.MODE_PRIVATE)
        val file = File(directory, filename)

        file.outputStream().use { outputStream ->
            outputStream.write(serialized.toByteArray())
        }
    }

    public fun loadProjectsFromFile(context: Context): ArrayList<Project> {
        val directory = context.getDir(projectsDirectory, Context.MODE_PRIVATE)
        val files = directory.listFiles()
        val projects = ArrayList<Project>()
        files?.forEach {
            val serialized = it.readText()
            val track = Json.decodeFromString<Project>(serialized)
            projects.add(track)
        }

        return projects
    }
}