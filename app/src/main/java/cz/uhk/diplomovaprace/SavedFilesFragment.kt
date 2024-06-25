package cz.uhk.diplomovaprace

import ProjektAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.uhk.diplomovaprace.Project.ProjectManager

class SavedFilesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_saved_files, container, false)

        val projectManager = ProjectManager()

        // Získání seznamu projektů
        val projects = projectManager.loadProjectsFromFile(context)

        val recyclerView: RecyclerView = view.findViewById(R.id.projektyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ProjektAdapter(projects)

        return view
    }
}