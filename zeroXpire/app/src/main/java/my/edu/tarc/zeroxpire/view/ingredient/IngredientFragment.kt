package my.edu.tarc.zeroxpire.view.ingredient

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Canvas
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.MainActivity
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.databinding.FragmentIngredientBinding
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*


class IngredientFragment : Fragment(), IngredientClickListener {
    private lateinit var binding: FragmentIngredientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var requestQueue: RequestQueue

    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private val goalViewModel: GoalViewModel by activityViewModels()

    private var isSort: Boolean = false
    private var id: Int = 0

    private var progressDialog: ProgressDialog? = null
    private lateinit var bottomSheetView: View
    private lateinit var bottomSheetDialog: BottomSheetDialog

    val CHANNEL_ID = "channelID"
    val CHANNEL_NAME = "channelName"
    val NOTIF_ID = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        binding = FragmentIngredientBinding.inflate(inflater, container, false)

        requestQueue = Volley.newRequestQueue(requireContext())


        return binding.root
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)





//        val mainActivity = activity as? MainActivity
//        mainActivity?.loadIngredient()

        binding.sortBtn.setBackgroundResource(R.drawable.baseline_sort_24)

        val adapter = IngredientAdapter(this, goalViewModel)

        loadIngredient(adapter)

        loadIngredientViewModel(adapter)



        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter




        sortIngredient(adapter)

        binding.filterBtn.setOnClickListener{
            filterIngredient(adapter)
        }

        searchIngredient(adapter)
        delete(adapter)
        navigateBack()
    }

    private fun loadIngredientViewModel(adapter: IngredientAdapter) {
        ingredientViewModel.ingredientList.observe(viewLifecycleOwner, Observer { ingredients ->
            goalViewModel.goalList.observe(viewLifecycleOwner, Observer { goals ->
                val unconsumedIngredients = ingredients.filter { ingredient ->
                    val goal = goals.find { it.goalId == ingredient.ingredientGoalId }
                    goal?.completedDate == null
                }

                if (unconsumedIngredients.isEmpty()) {
                    binding.emptyHereContent.visibility = View.VISIBLE
                    binding.notFoundContent.visibility = View.GONE  // Hide notFoundContent when the list is not empty
                } else {
                    binding.emptyHereContent.visibility = View.GONE
                    binding.notFoundContent.visibility = View.GONE  // Hide notFoundContent when the list is not empty
                    binding.labels.visibility = View.VISIBLE
                    binding.headers.visibility = View.VISIBLE
                    binding.recyclerview.visibility = View.VISIBLE
                    adapter.setIngredient(unconsumedIngredients)
                }
            })
        })
    }

    private fun filterIngredient(adapter: IngredientAdapter) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_filter, null)

        val resetButton = bottomSheetView.findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
            // Clear all filters and show the original list
            adapter.setIngredient(ingredientViewModel.ingredientList.value ?: emptyList())
            bottomSheetDialog.dismiss() // Close the bottom sheet after resetting
            binding.notFoundContent.visibility = View.GONE
        }
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        applyFilters(adapter, bottomSheetView)
    }

    private fun applyFilters(adapter: IngredientAdapter, bottomSheetView: View) {

        val expired = bottomSheetView.findViewById<RadioButton>(R.id.radioExpired)
        val notExpired = bottomSheetView.findViewById<RadioButton>(R.id.radioNotExpired)
        val isGoal = bottomSheetView.findViewById<RadioButton>(R.id.radioIsGoal)
        val isNotGoal = bottomSheetView.findViewById<RadioButton>(R.id.radioIsNotGoal)
        val vegChip = bottomSheetView.findViewById<Chip>(R.id.chipVegetables)
        val meatChip = bottomSheetView.findViewById<Chip>(R.id.chipMeats)
        val fruitsChip = bottomSheetView.findViewById<Chip>(R.id.chipFruits)
        val seafoodChip = bottomSheetView.findViewById<Chip>(R.id.chipSeafood)
        val eggProductsChip = bottomSheetView.findViewById<Chip>(R.id.chipEggProducts)
        val othersChip = bottomSheetView.findViewById<Chip>(R.id.chipOthers)

        val applyBtn = bottomSheetView.findViewById<Button>(R.id.applyButton)

        applyBtn.setOnClickListener {
            var filteredIngredients = ingredientViewModel.ingredientList.value ?: emptyList()

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)


            if (expired.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.expiryDate.before(today.time)
                }
            }

            if (notExpired.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.expiryDate.after(today.time) || ingredient.expiryDate == today.time
                }
            }

            if (isGoal.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientGoalId != null
                }
            }

            if (isNotGoal.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientGoalId == null
                }
            }

            if (vegChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Vegetables"
                }
            }

            if (meatChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Meat"
                }
            }

            if (fruitsChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Fruits"
                }
            }

            if (seafoodChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Seafood"
                }
            }

            if (eggProductsChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Eggs Products"
                }
            }

            if (othersChip.isChecked) {
                filteredIngredients = filteredIngredients.filter { ingredient ->
                    ingredient.ingredientCategory == "Others"
                }
            }

            if(filteredIngredients.isEmpty()){
                binding.notFoundContent.visibility = View.VISIBLE
            }
            else{
                binding.notFoundContent.visibility = View.GONE
            }

            filteredIngredients = filterConsumedIngredients(filteredIngredients)
            adapter.setIngredient(filteredIngredients)

            bottomSheetDialog.dismiss()
        }
    }

    private fun filterConsumedIngredients(ingredients: List<Ingredient>): List<Ingredient> {
        return ingredients.filter { ingredient ->
            val goal = goalViewModel.goalList.value?.find { it.goalId == ingredient.ingredientGoalId }
            goal?.completedDate == null
        }
    }

    private fun isIngredientConsumed(ingredient: Ingredient): Boolean {
        val goal = goalViewModel.goalList.value?.find { it.goalId == ingredient.ingredientGoalId }
        return goal?.completedDate != null
    }
    private fun sortIngredient(adapter: IngredientAdapter) {
        var sortMode = SortMode.BY_EXPIRY_DATE_ASC

        binding.sortBtn.setOnClickListener {
            when (sortMode) {
                SortMode.BY_EXPIRY_DATE_ASC -> {
                    ingredientViewModel.sortByExpiryDate().observe(viewLifecycleOwner, Observer {
                        val unconsumedIngredients = filterConsumedIngredients(it)
                        adapter.setIngredient(unconsumedIngredients)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable._023915_sort_ascending_fill_icon)
                    sortMode = SortMode.BY_EXPIRY_DATE_DESC
                }
                SortMode.BY_EXPIRY_DATE_DESC -> {
                    ingredientViewModel.sortByExpiryDateDesc().observe(viewLifecycleOwner, Observer {
                        val unconsumedIngredients = filterConsumedIngredients(it)
                        adapter.setIngredient(unconsumedIngredients)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable._023914_sort_descending_fill_icon)
                    sortMode = SortMode.BY_DATE_ADDED
                }
                SortMode.BY_DATE_ADDED -> {
                    ingredientViewModel.sortByDateAdded().observe(viewLifecycleOwner,  Observer {
                        val unconsumedIngredients = filterConsumedIngredients(it)
                        adapter.setIngredient(unconsumedIngredients)
                    })
                    binding.sortBtn.setBackgroundResource(R.drawable.baseline_sort_24)
                    sortMode = SortMode.BY_EXPIRY_DATE_ASC
                }
            }
        }
    }
    enum class SortMode {
        BY_EXPIRY_DATE_ASC,
        BY_EXPIRY_DATE_DESC,
        BY_DATE_ADDED
    }

    fun loadIngredient(adapter: IngredientAdapter?) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Loading...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_ingredient) + "?userId=${auth.currentUser?.uid}"
        Log.d("uid", auth.currentUser?.uid.toString())
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        if (ingredientViewModel.ingredientList.value?.isNotEmpty()!!) {
                            ingredientViewModel.deleteAllIngredients()
                        }
                        Log.d("Size", size.toString())


                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonIngredient: JSONObject = jsonArray.getJSONObject(i)
                                val ingredientId = jsonIngredient.getInt("ingredientId")
                                val ingredientName = jsonIngredient.getString("ingredientName")
                                val expiryDateString = jsonIngredient.getString("expiryDate")
                                val expiryDate =
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDateString)
                                val expiryDateInMillis = expiryDate?.time ?: 0L
                                val dateAddedString = jsonIngredient.getString("dateAdded")
                                val addedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateAddedString)
                                val dateAddedInMillis = addedDate?.time ?: 0L
                                val ingredientImage = jsonIngredient.getString("ingredientImage").replace("&amp;", "&")
                                Log.d("decode", ingredientImage)
                                val ingredientCategory = jsonIngredient.getString("ingredientCategory")
                                val isDelete = jsonIngredient.getInt("isDelete")
                                val goalId = jsonIngredient.optInt("goalId", 0)
                                val userId = jsonIngredient.getString("userId")
                                val ingredient: Ingredient

                                if(isDelete == 0){
                                    if (goalId == 0) {
                                        ingredient = Ingredient(
                                            ingredientId,
                                            ingredientName,
                                            Date(expiryDateInMillis),
                                            Date(dateAddedInMillis),
                                            ingredientImage,
                                            ingredientCategory,
                                            isDelete,
                                            null,
                                            userId// Set goalId to null when it is 0
                                        )
                                    } else {
                                        ingredient = Ingredient(
                                            ingredientId,
                                            ingredientName,
                                            Date(expiryDateInMillis),
                                            Date(dateAddedInMillis),
                                            ingredientImage,
                                            ingredientCategory,
                                            isDelete,
                                            goalId, // Set goalId to its value when it is not 0
                                            userId
                                        )
                                    }
                                    ingredientViewModel.addIngredient(ingredient)
                                    Log.d("IngredientCategory", ingredient.ingredientCategory)
                                }
                            }
                        }

                        // Dismiss the progress dialog when finished loading ingredients
                        progressDialog?.dismiss()

                        if (ingredientViewModel.ingredientList.value?.size == 0) {
                            binding.emptyHereContent.visibility = View.VISIBLE
                            binding.notFoundContent.visibility = View.GONE  // Hide notFoundContent when the list is not empty
                            binding.headers.visibility = View.GONE
                            binding.labels.visibility = View.GONE
                        } else {
                            binding.emptyHereContent.visibility = View.GONE
                            binding.notFoundContent.visibility = View.GONE  // Hide notFoundContent when the list is not empty
                            binding.labels.visibility = View.VISIBLE
                            binding.headers.visibility = View.VISIBLE
                        }
                        adapter?.setIngredient(ingredientViewModel.ingredientList.value ?: emptyList())


                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                    progressDialog?.dismiss()
                } catch (e: Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")
                    progressDialog?.dismiss()
                }
            },
            { error ->
                //i think is when there is nothing to return then it will return 404
                ingredientViewModel.deleteAllIngredients()
                progressDialog?.dismiss()
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)

    }


    private fun searchIngredient(adapter: IngredientAdapter) {
        binding.ingredientSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val filteredIngredients = ingredientViewModel.ingredientList.value?.filter { ingredient ->
                    !isIngredientConsumed(ingredient) && ingredient.ingredientName.contains(newText, ignoreCase = true)
                }

                if (filteredIngredients.isNullOrEmpty()) {
                    binding.notFoundContent.visibility = View.VISIBLE
                    binding.allIngredientsTextView.visibility = View.VISIBLE
                } else {
                    binding.notFoundContent.visibility = View.GONE
                    binding.allIngredientsTextView.visibility = View.VISIBLE
                }

                adapter.setIngredient(filteredIngredients ?: emptyList())

                return true
            }
        })
    }

    private fun navigateBack() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to Exit the app?").setCancelable(false)
                    .setPositiveButton("Exit") { dialog, id ->
                        requireActivity().finish()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )
    }
    private fun delete(adapter: IngredientAdapter) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                val goalName = checkDependency(viewHolder.adapterPosition, adapter)
                val builder = AlertDialog.Builder(requireContext())
                if(goalName.isEmpty()){
                    builder.setMessage("Are you sure you want to Delete?").setCancelable(false)
                        .setPositiveButton("Delete") { dialog, id ->
                            progressDialog = ProgressDialog(requireContext())
                            progressDialog?.setMessage("Deleting...")
                            progressDialog?.setCancelable(false)
                            progressDialog?.show()
                            val position = viewHolder.adapterPosition
                            val deletedIngredient = adapter.getIngredientAt(position)
                            ingredientViewModel.deleteIngredient(deletedIngredient)
                            val url = getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + deletedIngredient.ingredientId
                            Log.d("id:::::::::::::", deletedIngredient.ingredientId.toString())
                            val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                                { response ->
                                    // Handle successful deletion response, if required
                                    progressDialog?.dismiss()

                                    loadIngredient(adapter)
                                    adapter.notifyDataSetChanged()
                                    toast("Ingredient deleted.")
                                },
                                { error ->
                                    // Handle error response, if required
                                    Log.d("FK", "Error Response: ${error.message}")
                                }
                            )

                            requestQueue.add(jsonObjectRequest)
                        }.setNegativeButton("Cancel") { dialog, id ->
                            adapter.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }
                else{
                    builder.setMessage("This ingredient is currently in the goal \"$goalName\". " +
                            "\n\nDeleting this ingredient will also delete goal \"$goalName\"." +
                    "\n\n Are you sure want to delete?").setCancelable(false)
                        .setPositiveButton("Delete") { dialog, id ->
                            progressDialog = ProgressDialog(requireContext())
                            progressDialog?.setMessage("Deleting goal \"$goalName\"...")
                            progressDialog?.setCancelable(false)
                            progressDialog?.show()
                            val position = viewHolder.adapterPosition
                            val deletedIngredient = adapter.getIngredientAt(position)
                            val urlDeleteGoal = getString(R.string.url_server) + getString(R.string.url_delete_goal) + "?goalId=" + deletedIngredient.ingredientGoalId
                            val jsonObjectRequestDeleteGoal = JsonObjectRequest(Request.Method.POST, urlDeleteGoal, null,
                                { response ->
                                    clearGoalIdForIngredient(deletedIngredient)
                                    adapter.notifyDataSetChanged()
                                },
                                { error ->
                                    Log.d("Error", "Error Response: ${error.message}")
                                }
                            )
                            requestQueue.add(jsonObjectRequestDeleteGoal)
                        }.setNegativeButton("Cancel") { dialog, id ->
                            adapter.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                ).addBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.secondaryColor
                    )
                ).addActionIcon(R.drawable.baseline_delete_24).create().decorate()
                super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerview)
    }

    private fun clearGoalIdForIngredient(ingredient: Ingredient) {
        progressDialog?.setMessage("Deleting ingredient \"${ingredient.ingredientName}\"...")
        progressDialog?.setCancelable(false)
        val url = getString(R.string.url_server) + getString(R.string.url_clearGoalIdForIngredient_goal) + "?goalId=" + ingredient.ingredientGoalId
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->

                val url = getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + ingredient.ingredientId
                val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                    { response ->
                        // Handle successful deletion response, if required

                        progressDialog!!.dismiss()
                        toast("Ingredient and goal has deleted successfully.")



                    },
                    { error ->
                        // Handle error response, if required
                        Log.d("FK", "Error Response: ${error.message}")
                    }
                )

                requestQueue.add(jsonObjectRequest)
            },
            { error ->
                // Handle error response, if required
                Log.d("FK", "Error Response: ${error.message}")
            }
        )
        requestQueue.add(jsonObjectRequest)
    }


    private fun checkDependency(position: Int, adapter: IngredientAdapter): String {
        logg("position: $position")
        goalViewModel.goalList.value?.map {
            if(adapter.getIngredientAt(position).ingredientGoalId == it.goalId){
                logg("Comparison: ${adapter.getIngredientAt(position).ingredientGoalId} == ${it.goalId}")
                return it.goalName
            }

        }
        return ""

    }

    private fun logg(msg: String){
        Log.d("IngredientFragment", msg)
    }

    override fun onIngredientClick(ingredient: Ingredient) {
        findNavController().navigate(R.id.action_ingredientFragment_to_ingredientDetailFragment)

        //pass data to detail fragment
        setFragmentResult("requestName", bundleOf("name" to ingredient.ingredientName))
        setFragmentResult(
            "requestDate", bundleOf(
                "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ingredient.expiryDate)
            )
        )
        setFragmentResult("requestId", bundleOf("id" to ingredient.ingredientId))
        setFragmentResult("requestImage", bundleOf("image" to ingredient.ingredientImage))
        setFragmentResult("requestCategory", bundleOf("category" to ingredient.ingredientCategory))
        disableBtmNav()
    }
    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
