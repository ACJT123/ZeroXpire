package my.edu.tarc.zeroxpire.recipe

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecipeRecyclerViewItemDecoration(space: Int) : RecyclerView.ItemDecoration() {
    private var space: Int? = space

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space!!/2
        outRect.right = space!!/2
        outRect.bottom = space!!
        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0 || parent.getChildLayoutPosition(view) == 1) {
            outRect.top = space!!
        }else {
            outRect.top = 0
        }
    }
}