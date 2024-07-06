package cz.uhk.diplomovaprace

import ProjectAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.uhk.diplomovaprace.Project.ProjectManager
import cz.uhk.diplomovaprace.Project.ProjectViewModel

class SavedFilesFragment : Fragment() {

    private val viewModel: ProjectViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_saved_files, container, false)
        /*val newProjectLayout: LinearLayout = view.findViewById(R.id.newProjectLayout)

        // Nastavte listener pro kliknutí
        newProjectLayout.setOnClickListener {// Zavolejte funkci loadClearProject
            // ...

            // Navigujte na PianoRollFragment
            findNavController().navigate(R.id.action_savedFilesFragment_to_pianoRollFragment)
        }*/





        val projectManager = ProjectManager()

        // Získání seznamu projektů
        val projects = projectManager.loadProjectsFromFile(context)

        val recyclerView: RecyclerView = view.findViewById(R.id.projektyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ProjectAdapter(projects)

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val childView = rv.findChildViewUnder(e.x,e.y)
                if (childView != null && rv.getChildViewHolder(childView) is ProjectAdapter.ProjektViewHolder) {
                    when (e.action) {
                        MotionEvent.ACTION_UP -> {
                            val position = rv.getChildAdapterPosition(childView)
                            val project = projects[position]

                            // Uložte vybraný projekt do ViewModel
                            viewModel.selectProject(project)

                            // Navigujte na PianoRollFragment
                            findNavController().navigate(R.id.action_savedFilesFragment_to_pianoRollFragment)
                        }}
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        return view
    }
}