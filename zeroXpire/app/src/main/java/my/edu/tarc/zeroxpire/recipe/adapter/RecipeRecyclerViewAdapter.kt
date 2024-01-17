package my.edu.tarc.zeroxpire.recipe.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Recipe
import my.edu.tarc.zeroxpire.recipe.fragment.RecipeFragmentDirections
import my.edu.tarc.zeroxpire.recipe.viewHolder.RecipeRecyclerViewHolder


class RecipeRecyclerViewAdapter(
    private val recipeArrayList: ArrayList<Recipe>,
    ) : RecyclerView.Adapter<RecipeRecyclerViewHolder>() {

    // declaration
    private lateinit var parentContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeRecyclerViewHolder {
        parentContext = parent.context

        val view = LayoutInflater.from(parentContext).inflate(viewType, parent, false)
        return RecipeRecyclerViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.recipe_frame
    }

    override fun onBindViewHolder(holder: RecipeRecyclerViewHolder, position: Int) {
        // declaration: views
        val currentRecipe = recipeArrayList[position]

        val recipeDescConstraintLayout : ConstraintLayout = holder.getView().findViewById(R.id.recipeDescConstraintLayout)
        val titleTextView: TextView = holder.getView().findViewById(R.id.recipe_title_textview)
        val ingredientsTextView: TextView = holder.getView().findViewById(R.id.recipe_ingredients_textview)
        val recipeImageView : ImageView = holder.getView().findViewById(R.id.recipe_imageView)
        val recipeAuthorTextView: TextView = holder.getView().findViewById(R.id.recipeAuthorTextView)


        // navigation
        recipeDescConstraintLayout.setOnClickListener {
            val action = RecipeFragmentDirections.actionRecipeFragmentToRecipeDetails(currentRecipe.recipeID.toString())
            Navigation.findNavController(holder.getView()).navigate(action)
        }

        // set view
        ingredientsTextView.text = currentRecipe.ingredientNames

        titleTextView.text = currentRecipe.title

        recipeAuthorTextView.text = parentContext.getString(R.string.author, currentRecipe.authorName)

        Picasso.get().load(currentRecipe.imageLink).fit().centerCrop().into(recipeImageView)
    }


    override fun getItemCount(): Int {
        return recipeArrayList.size
    }

}




//bookmark button
//        bookmarkButton.setOnClickListener {
//            // if already exist in database
//            if (bookmarksDatabaseReference?.equalTo(recipeID.toString()).
//                on("bookmark_exists", fun() {}))
//            {
//                bookmarksDatabaseReference?.child(recipeID.toString())?.
//                setValue(recipeID)?.addOnCompleteListener {
//                    Toast.makeText(parentContext, "Added to bookmarks", Toast.LENGTH_SHORT).show()
//                }
//            } else
//            {
//                //set
//                bookmarksDatabaseReference?.child(recipeID.toString())?.
//                setValue(recipeID)?.addOnCompleteListener {
//                    Toast.makeText(parentContext, "Added to bookmarks", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }


//title
//        currentRecipe?.child("title")?.get()?.addOnCompleteListener { recipeTitle ->
//            titleTextView.text = recipeTitle.result.value.toString()
//        }
//
//        //ingredients
//        currentRecipe?.child("ingredients")?.get()?.addOnCompleteListener {recipeIngredients ->
//            ingredientsTextView.text = recipeIngredients.result.value.toString()
//        }
//
//        //image
//        currentRecipe?.child("image")?.addListenerForSingleValueEvent(
//            object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    // getting a DataSnapshot for the location at the
//                    // specified relative path and getting in the link variable
//                    val link = dataSnapshot.getValue(String::class.java)
//                    if (link != null) {
//                        // loading that data into recipeImageView
//                        // variable which is ImageView
//                        Picasso.get().load(link).into(recipeImageView)
//                    }else {
//                        recipeImageView.setImageResource(R.drawable.baseline_image_24)
//                    }
//                }
//
//                // this will called when any problem occurs in getting data
//                override fun onCancelled(databaseError: DatabaseError) {
//                    recipeImageView.setImageResource(R.drawable.baseline_broken_image_24)
//                }
//            }
//        )