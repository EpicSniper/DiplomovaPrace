import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.uhk.diplomovaprace.PianoRoll.Midi.Project
import cz.uhk.diplomovaprace.R

class ProjektAdapter(private val projects: List<Project>) :
    RecyclerView.Adapter<ProjektAdapter.ProjektViewHolder>() {

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
        holder.nazevTextView.text = project.getName()
        holder.popisTextView.text = project.getCreatedAt()
    }

    override fun getItemCount(): Int = projects.size
}