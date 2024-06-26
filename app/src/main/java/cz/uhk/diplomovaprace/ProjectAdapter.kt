import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.uhk.diplomovaprace.Project.Project
import cz.uhk.diplomovaprace.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProjectAdapter(private val projects: List<Project>) :
    RecyclerView.Adapter<ProjectAdapter.ProjektViewHolder>() {

    class ProjektViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazevTextView: TextView = itemView.findViewById(R.id.nazevProjektuTextView)
        val popisTextView: TextView = itemView.findViewById(R.id.popisProjektuTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjektViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.project_item, parent, false)
        return ProjektViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProjektViewHolder, position: Int) {
        val project = projects[position]
        //TODO: nastavit delku projektu
        holder.nazevTextView.text = project.getCreatedAt()
        holder.popisTextView.text = getFormattedDateTime(project.getCreatedAt())
    }

    override fun getItemCount(): Int = projects.size

    private fun getFormattedDateTime(dateTime: String): String {
        val localDateTime = LocalDateTime.parse(dateTime)
        val formatter = DateTimeFormatter.ofPattern("HH:mm, d MMM yyyy")
        return localDateTime.format(formatter)
    }
}