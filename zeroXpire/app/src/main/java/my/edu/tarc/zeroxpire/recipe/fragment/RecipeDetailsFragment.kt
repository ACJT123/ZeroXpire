package my.edu.tarc.zeroxpire.recipe.fragment

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.*
import my.edu.tarc.zeroxpire.recipe.adapter.CommentRecyclerViewAdapter
import my.edu.tarc.zeroxpire.recipe.viewModel.BookmarkViewModel
import my.edu.tarc.zeroxpire.recipe.viewModel.CommentViewModel
import my.edu.tarc.zeroxpire.recipe.viewModel.RecipeDetailsViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader


class RecipeDetailsFragment : Fragment() {
    private val requestCode = 1232
    private var numSteps = 1
    private lateinit var recipeID: String

    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String


    private lateinit var currentView: View
    private var recipe = Recipe()
    private var utilities = Utilities()

    private lateinit var editImageView: ImageView
    private lateinit var deleteImageView: ImageView
    private lateinit var commentImageView: ImageView
    private lateinit var bookmarkImageView: ImageView
    private lateinit var printImageView: ImageView
    private lateinit var saveImageView: ImageView
    private lateinit var addIngredientImageView: ImageView
    private lateinit var addInstructionsImageView: ImageView
    private lateinit var closeBtn: ImageView
    private lateinit var recipeDetailsRelativeLayout: RelativeLayout
    private lateinit var recipeDetailsInstructionsLinearLayout: LinearLayout
    private lateinit var recipeDetailsIngredientsLinearLayout: LinearLayout
    private lateinit var appBarEditText: EditText
    private lateinit var noteEditText: EditText
    private lateinit var noCommentTextView: TextView
    private lateinit var commentRecyclerView: RecyclerView

    //drawer
    private lateinit var commentEditText: EditText
    private lateinit var sendImageView: ImageView

    private val ingredientsCheckBoxArrayList = ArrayList<CheckBox>()
    private val instructionsEditTextArrayList = ArrayList<EditText>()
    private val linearLayoutArrayList = ArrayList<LinearLayout>()

    private val recipeDetailsViewModel = RecipeDetailsViewModel()

    private lateinit var drawerLayout: DrawerLayout
    private val commentViewModel = CommentViewModel()
    private var commentArrayList = ArrayList<Comment>()

    private val bookmarkViewModel = BookmarkViewModel()

    private lateinit var fileUri: Uri

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(R.layout.fragment_recipe_details, container, false)
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()
        disableBtmNav()

        initView()

        sendImageView.isEnabled = false
        sendImageView.isClickable = false

        commentRecyclerView.setHasFixedSize(true)
        commentRecyclerView.layoutManager = LinearLayoutManager(currentView.context)
        commentRecyclerView.addItemDecoration(CommentRecyclerViewItemDecoration(20))

        val upBtn = currentView.findViewById<ImageView>(R.id.upBtn)
        val appBar = currentView.findViewById<AppBarLayout>(R.id.appBar)

        val args: RecipeDetailsFragmentArgs by navArgs()
        recipeID = args.recipeID

        appBarEditText.inputType = InputType.TYPE_NULL
        noteEditText.inputType = InputType.TYPE_NULL

        //top bar back button
        upBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        commentImageView.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
            drawerLayout.openDrawer(GravityCompat.END)
        }

        printImageView.setOnClickListener {
            //setup layout
            askPermissions()
            appBar.isGone = true

            val recipeDetailsTitleTextView =
                currentView.findViewById<TextView>(R.id.recipeDetailsTitleEditText)
            recipeDetailsTitleTextView.text = recipe.title
            recipeDetailsTitleTextView.isGone = false

            val displayMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                currentView.display.getRealMetrics(displayMetrics)
            } else activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

            val fileName = "${recipe.title} recipe.pdf"

            recipeDetailsViewModel.convertXmlToPdf(currentView, displayMetrics, fileName)

            //return to normal layout
            appBar.isGone = false
            recipeDetailsTitleTextView.isGone = true
        }

        bookmarkImageView.setOnClickListener {
            if (recipe.isBookmarked) {
                bookmarkImageView.setImageResource(R.drawable.baseline_bookmark_border_24)
                ImageViewCompat.setImageTintList(
                    bookmarkImageView, ColorStateList.valueOf(
                        ContextCompat.getColor(currentView.context, R.color.btnColor)
                    )
                )
                removeBookmark()
            } else {
                bookmarkImageView.setImageResource(R.drawable.baseline_bookmark_24)
                ImageViewCompat.setImageTintList(
                    bookmarkImageView, ColorStateList.valueOf(
                        ContextCompat.getColor(currentView.context, R.color.btnColor)
                    )
                )
                setBookmark()
            }
        }

        recipeDetailsViewModel.getRecipeById(userID, recipeID, currentView) {
            recipe = it

            if (recipe.authorID == userID) {
                deleteImageView.isGone = false
                editImageView.isGone = false
            } else {
                deleteImageView.isGone = true
                editImageView.isGone = true
            }

            if (recipe.isBookmarked) {
                bookmarkImageView.setImageResource(R.drawable.baseline_bookmark_24)
                ImageViewCompat.setImageTintList(
                    bookmarkImageView, ColorStateList.valueOf(
                        ContextCompat.getColor(currentView.context, R.color.btnColor)
                    )
                )
            } else {
                bookmarkImageView.setImageResource(R.drawable.baseline_bookmark_border_24)
                ImageViewCompat.setImageTintList(
                    bookmarkImageView, ColorStateList.valueOf(
                        ContextCompat.getColor(currentView.context, R.color.btnColor)
                    )
                )
            }

            displayIngredients()
            displayInstructions()

            //set title and note
            appBarEditText.setText(recipe.title)
            noteEditText.setText(recipe.note)

            //display image
            Picasso.get().load(recipe.imageLink)
                .into(currentView.findViewById<ImageView>(R.id.recipeDescImageView))
        }

        loadComments()




        editImageView.setOnClickListener {
            val action = RecipeDetailsFragmentDirections.actionRecipeDetailsFragmentToRecipeCreateFragment().setRecipeID(recipe.recipeID.toString())
            Navigation.findNavController(currentView).navigate(action)
        }

        deleteImageView.setOnClickListener {
            val rootView = activity?.findViewById<View>(android.R.id.content)
            val alertDialogBuilder = utilities.createDeleteAlert(currentView.context)

            //Delete button
            alertDialogBuilder.setPositiveButton(R.string.delete_positive) { _, _ ->
                recipeDetailsViewModel.deleteRecipe(recipeID, 1, currentView) {
                    if (it) {
                        val snackBar = Snackbar.make(
                            currentView,
                            "Deleted recipe successfully",
                            Snackbar.LENGTH_SHORT
                        )
                        snackBar.setAction("UNDO",
                            UndoListener {
                                recipeDetailsViewModel.deleteRecipe(recipeID, 0, currentView) {
                                    if (rootView != null) {
                                        Snackbar.make(
                                            rootView,
                                            "Recipe restored successfully",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                        snackBar.show()
                        findNavController().popBackStack()
                    } else {
                        Snackbar.make(currentView, "Failed to delete recipe", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }

            }
            //Cancel button
            alertDialogBuilder.setNegativeButton(R.string.delete_negative) { _, _ ->
                //do nothing
            }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

        }

        // drawer layout
        recipeDetailsRelativeLayout.setOnTouchListener(object :
            OnSwipeTouchListener(currentView.context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
                drawerLayout.openDrawer(GravityCompat.END)
            }
        })

        commentEditText.doOnTextChanged { text, start, before, count ->
            sendImageView.isEnabled = !text.isNullOrBlank()
            sendImageView.isClickable = !text.isNullOrBlank()
        }


        sendImageView.setOnClickListener {
            val comment = Comment(
                recipeID = recipeID.toInt(),
                userID = userID,
                comment = commentEditText.text.toString(),
            )
            commentEditText.setText("")
            commentViewModel.createComment(comment, currentView) {
                if (it) {
                    loadComments()
                } else {
                    Snackbar.make(currentView,
                        "Comment failed to upload, try again later",
                        Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        closeBtn.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        return currentView
    }

    private fun createNewCheckBox(text: String): CheckBox {
        val newCheckBox = utilities.createNewCheckBox(currentView, text)
        newCheckBox.setOnClickListener {
            if (newCheckBox.isChecked) {
                newCheckBox.buttonDrawable = AppCompatResources.getDrawable(currentView.context, R.drawable.baseline_check_box_24)
            }else {
                newCheckBox.buttonDrawable = AppCompatResources.getDrawable(currentView.context, R.drawable.baseline_check_box_outline_blank_24)
            }
        }
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0,8, 0, 8)

        newCheckBox.layoutParams = layoutParams
        return newCheckBox
    }


    private fun displayIngredients() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                recipe.ingredientNamesArrayList.forEach {
                    val newCheckBox = createNewCheckBox(it)
                    ingredientsCheckBoxArrayList.add(newCheckBox)
                    recipeDetailsIngredientsLinearLayout.addView(newCheckBox)
                }
            }
        }
    }

    private fun displayInstructions() {
        CoroutineScope(Dispatchers.IO).launch {
            val httpClient = OkHttpClient()
            val request = Request.Builder().url(recipe.instructionsLink).build()
            val response = httpClient.newCall(request).execute()
            val instructions = response.body?.string().toString()
            val reader = BufferedReader(StringReader(instructions))

            withContext(Dispatchers.Main) {
                reader.forEachLine {
                    createNewStep(it, currentView.context.getString(R.string.step_hint), false)
                }
            }
        }
    }


    private fun askPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, WRITE_EXTERNAL_STORAGE)) {
            Log.d("Write Permission", "Not need to show permission rationale")
        }else {
            Log.d("Write Permission", "Should show permission rationale")
        }

        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(WRITE_EXTERNAL_STORAGE),
            requestCode
        )

        val writeStoragePermission = ContextCompat.checkSelfPermission(
            currentView.context,
            WRITE_EXTERNAL_STORAGE
        )

        if (writeStoragePermission == PackageManager.PERMISSION_GRANTED) {
            Log.d("Write Permission", "Write permission has been granted")
        }
    }


    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun createNewStep(text: String = "", hint: String, editable: Boolean) {
        val newStepTextView = utilities.createNewTextView(currentView, "$numSteps. ", Typeface.BOLD)

        val newInstructionEditText = utilities.createNewEditText(currentView, text, hint)
        instructionsEditTextArrayList.add(newInstructionEditText)
        if (!editable) {
            newInstructionEditText.inputType = InputType.TYPE_NULL
        }

        val newLinearLayout = utilities.createNewLinearLayout(currentView, top = 8, bottom = 8)
        linearLayoutArrayList.add(newLinearLayout)
        newLinearLayout.addView(newStepTextView)
        newLinearLayout.addView(newInstructionEditText)

        recipeDetailsInstructionsLinearLayout.addView(newLinearLayout)
        numSteps++
    }

    private fun loadComments() {
        commentViewModel.getComments(recipeID.toInt(), currentView) {
            commentArrayList = it
            if (commentArrayList.isEmpty()) {
                commentRecyclerView.isGone = true
                noCommentTextView.isGone = false
            }else {
                commentRecyclerView.isGone = false
                noCommentTextView.isGone = true
                commentRecyclerView.adapter = CommentRecyclerViewAdapter(commentArrayList)
            }
        }
    }

    private fun initView() {
        appBarEditText = currentView.findViewById(R.id.appBarEditText)
        noteEditText = currentView.findViewById(R.id.noteEditText)
        recipeDetailsRelativeLayout = currentView.findViewById(R.id.recipeDetailsLinearLayout)
        recipeDetailsInstructionsLinearLayout = currentView.findViewById(R.id.recipeDetailsInstructionsLinearLayout)
        recipeDetailsIngredientsLinearLayout = currentView.findViewById(R.id.recipeDetailsIngredientsLinearLayout)
        printImageView = currentView.findViewById(R.id.printImageView)
        deleteImageView = currentView.findViewById(R.id.deleteImageView)
        editImageView = currentView.findViewById(R.id.editImageView)
        commentImageView = currentView.findViewById(R.id.commentImageView)
        bookmarkImageView = currentView.findViewById(R.id.bookmarkImageView)
        saveImageView = currentView.findViewById(R.id.saveImageView)
        addIngredientImageView = currentView.findViewById(R.id.addIngredientImageView)
        addInstructionsImageView = currentView.findViewById(R.id.addInstructionsImageView)
        drawerLayout = currentView.findViewById(R.id.drawerLayout)
        commentEditText = currentView.findViewById(R.id.commentEditText)
        noCommentTextView = currentView.findViewById(R.id.noCommentTextView)
        sendImageView = currentView.findViewById(R.id.sendImageView)
        commentRecyclerView = currentView.findViewById(R.id.commentRecyclerView)
        closeBtn = currentView.findViewById(R.id.closeBtn)
        fileUri = Uri.EMPTY
    }

    private fun removeBookmark() {
        bookmarkViewModel.removeFromBookmarks(userID, recipeID, currentView) {
            if (it) {
                recipe.isBookmarked = false

                // display result and undo button
                val snackBar = Snackbar.make(currentView, "Removed from bookmarks", Snackbar.LENGTH_SHORT)
                snackBar.setAction("UNDO",
                    UndoListener {
                        bookmarkViewModel.addToBookmark(userID, recipeID, currentView) {
                            bookmarkImageView.setImageResource(R.drawable.baseline_bookmark_24)
                            ImageViewCompat.setImageTintList(
                                bookmarkImageView, ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        currentView.context,
                                        R.color.btnColor
                                    )
                                )
                            )
                            recipe.isBookmarked = true
                        }

                    }
                )
                snackBar.show()
            }

        }
    }

    private fun setBookmark() {
        bookmarkViewModel.addToBookmark(userID, recipeID, currentView) {
            recipe.isBookmarked = true
            Snackbar.make(currentView, "Bookmarked successfully", Snackbar.LENGTH_SHORT).show()
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