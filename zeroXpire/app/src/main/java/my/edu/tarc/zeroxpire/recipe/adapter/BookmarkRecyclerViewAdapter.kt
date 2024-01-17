package my.edu.tarc.zeroxpire.recipe.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.fragment.BookmarksFragmentDirections
import my.edu.tarc.zeroxpire.recipe.viewHolder.BookmarkRecyclerViewHolder

class BookmarkRecyclerViewAdapter(
    private val recipeArrayList: ArrayList<Recipe>,
    ) : RecyclerView.Adapter<BookmarkRecyclerViewHolder>() {

    private var selectedPosArrayList = ArrayList<Int>()

    var isEditing = false

    // declaration
    private lateinit var parentContext: Context
    private lateinit var auth: FirebaseAuth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkRecyclerViewHolder {
        parentContext = parent.context
        auth = FirebaseAuth.getInstance()

        val view = LayoutInflater.from(parentContext).inflate(viewType, parent, false)
        return BookmarkRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.bookmark_frame
    }

    override fun onBindViewHolder(holder: BookmarkRecyclerViewHolder, position: Int) {
        // declaration: views
        val currentRecipe = recipeArrayList[position]
        val currentView = holder.getView()

        val bookmarkFrameOverlay : LinearLayout = currentView.findViewById(R.id.bookmarkFrameOverlay)
        val bookmarkConstraintLayout : ConstraintLayout = currentView.findViewById(R.id.bookmarkConstraintLayout)
        val innerConstraintLayout : ConstraintLayout = currentView.findViewById(R.id.innerConstraintLayout)
        val bookmarkTitleTextview: TextView = currentView.findViewById(R.id.bookmarkTitleTextview)
        val bookmarkIngredientsTextview: TextView = currentView.findViewById(R.id.bookmarkIngredientsTextview)
        val bookmarkImageView : ImageView = currentView.findViewById(R.id.bookmarkImageView)

        bookmarkConstraintLayout.setOnClickListener{
            val action = BookmarksFragmentDirections.actionBookmarksToRecipeDetails(currentRecipe.recipeID.toString())
            Navigation.findNavController(currentView).navigate(action)
        }

        bookmarkFrameOverlay.isGone = !isEditing

        bookmarkTitleTextview.text = currentRecipe.title

        bookmarkIngredientsTextview.text = currentRecipe.ingredientNames

        Picasso.get().load(currentRecipe.imageLink).into(bookmarkImageView)

        bookmarkFrameOverlay.setOnClickListener {
            if (selectedPosArrayList.contains(position)) {
                selectedPosArrayList.remove(position)
                currentView.isSelected = false
            }else {
                selectedPosArrayList.add(position)
                currentView.isSelected = true
            }
            notifyItemChanged(position)
        }

        val color: ColorStateList
        if (selectedPosArrayList.contains(position)) {
            color = ContextCompat.getColorStateList(parentContext, R.color.btnColor)!!
            Log.d("bookmark selection", "Pos: $position, color: btnColor")
        }else {
            color = ContextCompat.getColorStateList(parentContext, R.color.white)!!
            Log.d("bookmark selection", "Pos: $position, color: white")
        }
        innerConstraintLayout.backgroundTintList = color
    }

    override fun getItemCount(): Int {
        return recipeArrayList.size
    }

    fun getSelectedPosArrayList(): ArrayList<Int> {
        return selectedPosArrayList
    }
}