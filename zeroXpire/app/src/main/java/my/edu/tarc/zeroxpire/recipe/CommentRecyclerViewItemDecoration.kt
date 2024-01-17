package my.edu.tarc.zeroxpire.recipe

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CommentRecyclerViewItemDecoration(private var space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = space
        outRect.bottom = space

        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space.times(2)
        }
        if (parent.getChildLayoutPosition(view) == parent.childCount) {
            outRect.bottom = space.times(2)
        }
    }
}