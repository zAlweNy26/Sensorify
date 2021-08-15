package it.alwe.sensorify

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class MissingListAdapter(var context: Context, var missingSensors: List<MissingSensor>) :
    RecyclerView.Adapter<MissingListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.sensorTitle.text = missingSensors[position].title
        holder.sensorInfo.text = missingSensors[position].description
        holder.sensorIcon.setImageResource(missingSensors[position].icon)

        val isExpanded: Boolean = missingSensors[position].isExpanded
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.toggleSensor.animate().setDuration(300).rotation(if (isExpanded) 180f else 0f)
    }

    override fun getItemCount(): Int { return missingSensors.size }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var expandableLayout: ConstraintLayout = itemView.findViewById(R.id.expandableLayout)
        var titleLayout: ConstraintLayout = itemView.findViewById(R.id.titleLayout)
        var sensorTitle: TextView = itemView.findViewById(R.id.sensorTitle)
        var sensorInfo: TextView = itemView.findViewById(R.id.sensorInfo)
        var sensorIcon: ImageButton = itemView.findViewById(R.id.sensorIcon)
        var toggleSensor: ImageButton = itemView.findViewById(R.id.toggleSensor)

        private val clickListener = View.OnClickListener { view ->
            missingSensors[adapterPosition].isExpanded = !missingSensors[adapterPosition].isExpanded
            toggleSensor.animate().setDuration(300).rotation(if (missingSensors[adapterPosition].isExpanded) 180f else 0f)
            notifyItemChanged(adapterPosition)
        }

        init {
            titleLayout.setOnClickListener(clickListener)
            sensorTitle.setOnClickListener(clickListener)
            sensorIcon.setOnClickListener(clickListener)
            toggleSensor.setOnClickListener(clickListener)
        }
    }
}