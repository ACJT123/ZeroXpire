package my.edu.tarc.zeroxpire.recipe.viewHolder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R

class RecipeRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val view: ConstraintLayout

    init {
        super.itemView
        view = itemView.findViewById(R.id.recipeRecyclerViewConstraintLayout)
    }

    fun getView(): ConstraintLayout {
        return view
    }
}
