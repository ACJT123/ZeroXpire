package my.edu.tarc.zeroxpire.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R

class RecognitionResultsAdapterName(
    private val context: Context,
    private val results: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RecognitionResultsAdapterName.ViewHolder>() {

    val selectedItems = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recognition_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.bind(result)
    }

    override fun getItemCount(): Int {
        return results.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(result: String) {
            val textView = itemView.findViewById<TextView>(R.id.textRecognitionResult)
            textView.text = result

            // Update UI based on selected state
            if (selectedItems.contains(result)) {
                itemView.setBackgroundResource(R.color.btnColor)
                textView.setTextColor(Color.WHITE)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
                textView.setTextColor(ActivityCompat.getColor(context, R.color.textColor))
            }

            itemView.setOnClickListener {
                if (selectedItems.contains(result)) {
                    selectedItems.remove(result)
                } else {
                    selectedItems.add(result)
                }
                onItemClick(result)
                notifyDataSetChanged() // Update the UI after selection change
            }
        }
    }


}
