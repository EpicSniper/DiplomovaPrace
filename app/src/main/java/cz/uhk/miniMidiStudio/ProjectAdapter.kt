import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import cz.uhk.miniMidiStudio.Project.Project
import cz.uhk.miniMidiStudio.Project.ProjectViewModel
import cz.uhk.miniMidiStudio.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProjectAdapter(private val projects: List<Project>, private val viewModel: ProjectViewModel, private val navController: NavController) :
    RecyclerView.Adapter<ProjectAdapter.ProjektViewHolder>() {

    inner class ProjektViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazevTextView: TextView = itemView.findViewById(R.id.nazevProjektuTextView)
        val popisTextView: TextView = itemView.findViewById(R.id.popisProjektuTextView)
        val menuButton: ImageView = itemView.findViewById(R.id.menuImage)

        init {
            val textViewsContainer = itemView.findViewById<LinearLayout>(R.id.textViewsContainer)
            textViewsContainer.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val project = projects[position] // Get the project at the clicked position
                    viewModel.selectProject(project) // Pass the selected project to your ViewModel (if needed)
                    navController.navigate(R.id.action_savedFilesFragment_to_pianoRollFragment) // Navigate to PianoRollFragment
                }
            }

            // Keep the click listener on menuButton for thePopupMenu
            menuButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMenuClickListener?.onMenuClick(it, position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjektViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.project_item, parent, false)
        return ProjektViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProjektViewHolder, position: Int) {
        val project = projects[position]
        //TODO: nastavit delku projektu
        holder.nazevTextView.text = project.getName()
        holder.popisTextView.text = getFormattedDateTime(project.getCreatedAt())
    }

    override fun getItemCount(): Int = projects.size

    private fun getFormattedDateTime(dateTime: String): String {
        val localDateTime = LocalDateTime.parse(dateTime)
        val formatter = DateTimeFormatter.ofPattern("HH:mm, d MMM yyyy")
        return localDateTime.format(formatter)
    }

    interface OnMenuClickListener {
        fun onMenuClick(view: View, position: Int)
    }

    private var onMenuClickListener: OnMenuClickListener? = null

    // Setter for the listener
    fun setOnMenuClickListener(listener: OnMenuClickListener) {
        this.onMenuClickListener = listener
    }
}