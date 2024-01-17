package my.edu.tarc.zeroxpire.recipe.viewHolder

import android.view.View
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R

class CommentRecyclerViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val view: CardView

    init {
        super.itemView
        view = itemView.findViewById(R.id.commentCardView)
    }

    fun getView(): CardView {
        return view
    }
}