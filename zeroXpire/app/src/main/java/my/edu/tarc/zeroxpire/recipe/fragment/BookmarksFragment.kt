package my.edu.tarc.zeroxpire.recipe.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.adapter.BookmarkRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.BookmarkViewModel


class BookmarksFragment : Fragment() {
    // declaration
    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    private lateinit var currentView: View

    private var isEditing = false

    private lateinit var upBtn: ImageView
    private lateinit var deleteImageView: ImageView
    private lateinit var editImageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var appBarEditText: EditText
    private lateinit var noBookmarksFoundTextView: TextView

    private lateinit var recyclerViewAdapter: BookmarkRecyclerViewAdapter

    private var recipeArrayList = ArrayList<Recipe>()

    private val bookmarkViewModel = BookmarkViewModel()

    private lateinit var swipeHelper: ItemTouchHelper

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe_bookmarks, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()
        disableBtmNav()

        // assign views
        upBtn = currentView.findViewById(R.id.upBtn)
        deleteImageView = currentView.findViewById(R.id.deleteImageView)
        editImageView = currentView.findViewById(R.id.editImageView)
        recyclerView = currentView.findViewById(R.id.bookmarksRecyclerView)
        appBarEditText = currentView.findViewById(R.id.appBarEditText)
        noBookmarksFoundTextView = currentView.findViewById(R.id.noBookmarksFoundTextView)

        loadBookmarks()

        val span = SpannableString(getString(R.string.no_bookmarks_found))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                findNavController().popBackStack()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE
                ds.isUnderlineText = false
            }
        }
        span.setSpan(clickableSpan, 20, 27, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        noBookmarksFoundTextView.text = span
        noBookmarksFoundTextView.movementMethod = LinkMovementMethod.getInstance()
        noBookmarksFoundTextView.highlightColor = Color.TRANSPARENT

        // navigation
        upBtn.setOnClickListener{
            findNavController().popBackStack()
        }

        appBarEditText.inputType = InputType.TYPE_NULL

        //setup recycler view
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(currentView.context)


        editImageView.setOnClickListener {
            isEditing = !isEditing
            if (isEditing) {
                //change icons
                deleteImageView.isGone = false
                editImageView.setImageResource(R.drawable.baseline_edit_off_24)

                recyclerViewAdapter.isEditing = true
            }else {
                //change icons
                deleteImageView.isGone = true
                editImageView.setImageResource(R.drawable.baseline_edit_24)

                recyclerViewAdapter.isEditing = false
            }
            ImageViewCompat.setImageTintList(
                editImageView, ColorStateList.valueOf(ContextCompat.getColor(currentView.context, R.color.btnColor)))
            recyclerViewAdapter.notifyDataSetChanged()
            recyclerView.adapter = recyclerViewAdapter
        }

        deleteImageView.setOnClickListener {
            val recipeIDArrayList = ArrayList<String>()
            val selectedPosArrayList = recyclerViewAdapter.getSelectedPosArrayList()

            selectedPosArrayList.forEach {
                recipeIDArrayList.add(recipeArrayList[it].recipeID.toString())
            }
            bookmarkViewModel.removeArrayFromBookmarks(userID, recipeIDArrayList, currentView) {
                val snackBar = Snackbar.make(currentView, "Removed from bookmarks", Snackbar.LENGTH_SHORT)
                snackBar.setAction("UNDO",
                    UndoListener {
                        bookmarkViewModel.addToBookmarksFromArray(userID, recipeIDArrayList, currentView) {
                            loadBookmarks()
                        }
                    }
                )
                snackBar.show()
            }
            selectedPosArrayList.sortDescending()
            selectedPosArrayList.forEach {
                recipeArrayList.removeAt(it)
            }
            recyclerViewAdapter = BookmarkRecyclerViewAdapter(recipeArrayList)
            recyclerView.adapter = recyclerViewAdapter
        }

        return currentView
    }


    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun setUpItemTouchHelper() {
        swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                val recipeID = recipeArrayList[pos].recipeID.toString()

                bookmarkViewModel.removeFromBookmarks(userID, recipeID, currentView) {
                    val snackBar = Snackbar.make(currentView, "Removed from bookmarks"
                        , Snackbar.LENGTH_SHORT)
                    snackBar.setAction("UNDO",
                        UndoListener {
                            bookmarkViewModel.addToBookmark(userID, recipeID, currentView) {
                                loadBookmarks()
                            }
                        }
                    )
                    snackBar.show()
                }

                recipeArrayList.removeAt(pos)
                recyclerViewAdapter = BookmarkRecyclerViewAdapter(recipeArrayList)
                recyclerView.adapter = recyclerViewAdapter
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                ).addBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.secondaryColor
                    )
                ).addActionIcon(R.drawable.baseline_delete_24).create().decorate()
                super.onChildDraw(
                    canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }
        })
        swipeHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadBookmarks() {
        bookmarkViewModel.getBookmarksByUserID(userID, currentView) {
            if (it.isNotEmpty()) {
                noBookmarksFoundTextView.isGone = true
                recyclerView.isGone = false

                recipeArrayList = it
                recyclerViewAdapter = BookmarkRecyclerViewAdapter(recipeArrayList)
                recyclerView.adapter = BookmarkRecyclerViewAdapter(recipeArrayList)

                setUpItemTouchHelper()
            }else {
                noBookmarksFoundTextView.isGone = false
                recyclerView.isGone = true
            }
        }
    }

    class UndoListener(
        private val callback: (Boolean) -> Unit
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            callback(true)
        }
    }

}