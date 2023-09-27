package my.edu.tarc.zeroxpire.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class IngredientAdapter(
    private val clickListener: IngredientClickListener,
    private val goalViewModel: GoalViewModel
) : RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    private var ingredientList = emptyList<Ingredient>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewIngredientName: TextView = view.findViewById(R.id.ingredientName)
        val textViewDaysLeft: TextView = view.findViewById(R.id.daysLeft)
        val textViewExpiryDate: TextView = view.findViewById(R.id.expiryDate)
        val textViewDateAdded: TextView = view.findViewById(R.id.dateAdded)
        val isAddedToGoalImage: ImageView = view.findViewById(R.id.isAddedToGoalImage)
        val imageViewIngredientCategoryImage: ImageView = view.findViewById(R.id.ingredientCategoryImage)
        val imageViewIngredientImage: ImageView = view.findViewById(R.id.ingredientImage)
    }

    internal fun setIngredient(ingredient: List<Ingredient>) {
        ingredientList = ingredient
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_design, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]

        holder.textViewIngredientName.text = ingredient.ingredientName

        // Set category image using Glide
        val categoryImageResource = when (ingredient.ingredientCategory) {
            "Vegetables" -> R.drawable.vegetable
            "Fruits" -> R.drawable.fruits
            "Meat" -> R.drawable.meat
            "Seafood" -> R.drawable.seafood
            "Eggs Products" -> R.drawable.eggs_2713474
            "Other" -> R.drawable.icons_other

            else -> {}
        }
        Glide.with(holder.itemView.context)
            .load(categoryImageResource)
            .into(holder.imageViewIngredientCategoryImage)

        // Set expiry date
        val expiryDate: LocalDate? = ingredient.expiryDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        val formattedExpiryDate: String? = expiryDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        holder.textViewExpiryDate.text = "Expiry Date: ${formattedExpiryDate.orEmpty()}"

        // Set days left
        if (expiryDate != null) {
            val currentDate: LocalDate = LocalDate.now()
            val absoluteDaysLeft: Long = ChronoUnit.DAYS.between(currentDate, expiryDate)
            // Set text and color based on days left
            val daysLeftText: String = when {
                absoluteDaysLeft < 0 -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.secondaryColor))
                    "Expired"
                }
                absoluteDaysLeft == 0L -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.textColor))
                    "Expires Today"
                }
                else -> {
                    holder.textViewDaysLeft.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
                    when (absoluteDaysLeft) {
                        1L -> "Expired in $absoluteDaysLeft day"
                        else -> "Expired in $absoluteDaysLeft days"
                    }
                }
            }
            holder.textViewDaysLeft.text = daysLeftText
        } else {
            holder.textViewDaysLeft.text = "Expiration Date Not Set"
        }

        val goal = goalViewModel.goalList.value
        goal?.map {
            if(it.goalId == ingredient.ingredientGoalId){
                if(it.completedDate != null){
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedCompletedDate = sdf.format(it.completedDate)
                    val formattedExpiryDate = sdf.format(ingredient.expiryDate)


                    holder.textViewExpiryDate.text = "Consumed at: $formattedCompletedDate"
                    holder.textViewExpiryDate.setTextColor(getColor(holder.itemView.context, R.color.btnColor))
                    holder.isAddedToGoalImage.visibility = View.INVISIBLE
                    holder.textViewDaysLeft.text = "Expiry Date: ${formattedExpiryDate}"
                }
            }
        }


        // Set date added
        val dateFormatter = SimpleDateFormat("d/M/yyyy")
        val addedDate: Date? = ingredient.dateAdded
        val formattedDate = addedDate?.let { dateFormatter.format(it) } ?: "Date Not Set"
        holder.textViewDateAdded.text = "Date Added: $formattedDate"

        // Load ingredient image using Glide
        if (!ingredient.ingredientImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(ingredient.ingredientImage)
                .into(holder.imageViewIngredientImage)
        }

        // Set goal image if added to goal
        if (ingredient.ingredientGoalId != null) {
            Glide.with(holder.itemView.context)
                .load(R.drawable.goal)
                .into(holder.isAddedToGoalImage)
        } else {
            holder.isAddedToGoalImage.setImageDrawable(null)
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            clickListener.onIngredientClick(ingredient)
        }
    }

    override fun getItemCount(): Int {
        return ingredientList.size
    }

    fun getIngredientAt(position: Int): Ingredient {
        return ingredientList[position]
    }

    fun getPosition(ingredient: Ingredient): Int {
        return ingredientList.indexOf(ingredient)
    }
}
