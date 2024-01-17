package my.edu.tarc.zeroxpire.adapters

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.goal.GoalClickListener
import my.edu.tarc.zeroxpire.model.Goal
import my.edu.tarc.zeroxpire.model.Ingredient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class GoalAdapter(private val clickListener: GoalClickListener) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {
    private var goalList = emptyList<Goal>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewGoalName: TextView = view.findViewById(R.id.goalName)
        val textViewDaysLeft: TextView = view.findViewById(R.id.daysLeft)
        val goalStateImage: ImageView = view.findViewById(R.id.goalStateImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.goal_recycler_view, parent, false)
        return ViewHolder(view)
    }

    internal fun setGoal(goal: List<Goal>) {
        goalList = goal
        notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goalList[position]
        holder.textViewGoalName.text = goal.goalName
        val currentDate = Calendar.getInstance().time
        val targetCompletionDate = goal.targetCompletionDate

        val differenceMillis = targetCompletionDate.time - currentDate.time
        val daysDifference = TimeUnit.MILLISECONDS.toDays(differenceMillis)

        val isCompleted: Boolean = goal.completedDate != null
        val daysLeftText: String = when {
            isCompleted -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.completed_goal)
                    .into(holder.goalStateImage)
                "Completed"
            }
            daysDifference >= 0L && currentDate.before(targetCompletionDate) -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.active_goal)
                    .into(holder.goalStateImage)
                when (daysDifference) {
                    0L -> {
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val formattedTime = timeFormat.format(targetCompletionDate)
                        "Due today at $formattedTime"
                    }
                    1L -> "Due tomorrow"
                    else -> "Due in $daysDifference days"
                }
            }
            else -> {
                holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                holder.textViewGoalName.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                Glide.with(holder.itemView.context)
                    .load(R.drawable.uncompleted_goal)
                    .into(holder.goalStateImage)
                "Overdue"
            }
        }

        holder.textViewDaysLeft.text = daysLeftText

        holder.itemView.setOnClickListener {
            clickListener.onGoalClick(goal)
        }
    }


    override fun getItemCount(): Int {
        return goalList.size
    }

    fun getGoalAt(position: Int): Goal {
        return goalList[position]
    }

    fun getPosition(goal: Goal): Int{
        return goalList.indexOf(goal)
    }

    private fun logg(msg:String){
        Log.d("GoalAdapter", msg)
    }

}
