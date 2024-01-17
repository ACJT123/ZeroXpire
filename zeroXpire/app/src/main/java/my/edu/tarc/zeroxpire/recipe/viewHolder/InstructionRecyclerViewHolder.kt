package my.edu.tarc.zeroxpire.recipe.viewHolder

import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R

class InstructionRecyclerViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView){
    private val view: LinearLayout

    init {
        super.itemView
        view = itemView.findViewById(R.id.instructionFrameLinearLayout)
    }

    fun getView(): LinearLayout {
        return view
    }

}