package cz.uhk.diplomovaprace.Project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProjectViewModel : ViewModel() {
    private val _selectedProject = MutableLiveData<Project>()
    val selectedProject: LiveData<Project> = _selectedProject

    fun selectProject(project: Project) {
        _selectedProject.value = project
    }
}