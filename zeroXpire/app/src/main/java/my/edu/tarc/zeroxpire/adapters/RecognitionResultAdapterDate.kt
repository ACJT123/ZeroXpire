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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class RecognitionResultsAdapterDate(
    private val context: Context,
    private val results: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RecognitionResultsAdapterDate.ViewHolder>() {

    private var selectedItem: String? = null

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
            val dateFormats = listOf("dd/MM/yy", "dd MM yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "dd.MM.yyyy")  // Add your desired date formats

            var parsedDate: Date? = null

            for (format in dateFormats) {
                val dateFormat = SimpleDateFormat(format)
                try {
                    parsedDate = dateFormat.parse(result)
                    if (parsedDate != null) {
                        break  // Break out of the loop if parsing is successful
                    }
                } catch (e: ParseException) {
                    // Continue to the next format if parsing fails
                }
            }

            if (parsedDate != null) {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy")  // Choose your desired output format
                val formattedDate = outputFormat.format(parsedDate)
                textView.text = formattedDate
            } else {
                textView.text = "Error parsing date"
            }


            // Update UI based on selected state
            if (selectedItem == result) {
                itemView.setBackgroundResource(R.color.btnColor)
                textView.setTextColor(Color.WHITE)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
                textView.setTextColor(ActivityCompat.getColor(context, R.color.textColor))
            }

            itemView.setOnClickListener {
                selectedItem = result
                onItemClick(result)
                notifyDataSetChanged() // Update the UI after selection change
            }
        }
    }
}
