package my.edu.tarc.zeroxpire.view.goal

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import my.edu.tarc.zeroxpire.MainActivity
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.adapters.GoalAdapter
import my.edu.tarc.zeroxpire.databinding.FragmentGoalBinding
import my.edu.tarc.zeroxpire.goal.GoalClickListener
import my.edu.tarc.zeroxpire.model.Goal
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

class GoalFragment : Fragment(), OnChartValueSelectedListener, GoalClickListener {
    private lateinit var binding: FragmentGoalBinding
    private val goalViewModel: GoalViewModel by activityViewModels()
    private val ingredientViewModel: IngredientViewModel by activityViewModels()
    private var progressDialog: ProgressDialog? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var pieChart: PieChart

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGoalBinding.inflate(inflater, container, false)

        requestQueue = Volley.newRequestQueue(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as? MainActivity
        //mainActivity?.loadIngredient()
        auth = FirebaseAuth.getInstance()

        pieChart = binding.pieChart

        val adapter = GoalAdapter(this)

        loadGoal(adapter)

        binding.goalRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.goalRecyclerview.adapter = adapter



        goalViewModel.goalList.observe(viewLifecycleOwner, Observer {goals ->
            adapter.setGoal(goals)
            pieChart()
            pieChartAnimation()
        })

        searchGoal(adapter)
        delete(adapter)
        navigateBack()

        markAsCompleted(adapter)

    }


    private fun searchGoal(adapter: GoalAdapter) {
        binding.goalSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val filteredGoals = goalViewModel.goalList.value?.filter { goal ->
                    goal.goalName.contains(newText, ignoreCase = true)
                }

                if (filteredGoals.isNullOrEmpty()) {
                    binding.emptyHereContent.visibility = View.VISIBLE
                    binding.allGoalsTextView.visibility = View.GONE
                    binding.goalRecyclerview.visibility = View.GONE
                    binding.goalStatistics.visibility = View.GONE
                    binding.pieChart.visibility = View.GONE
                    binding.legend.visibility = View.GONE
                } else {
                    binding.emptyHereContent.visibility = View.GONE
                    binding.allGoalsTextView.visibility = View.VISIBLE
                    binding.goalRecyclerview.visibility = View.VISIBLE
                    binding.goalStatistics.visibility = View.VISIBLE
                    binding.pieChart.visibility = View.VISIBLE
                    binding.legend.visibility = View.VISIBLE
                }

                adapter.setGoal(filteredGoals ?: emptyList())

                return true
            }
        })
    }

    private fun markAsCompleted(adapter: GoalAdapter){
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Complete this goal?").setCancelable(false)
                    .setPositiveButton("Complete") { dialog, id ->
                        progressDialog = ProgressDialog(requireContext())
                        progressDialog?.setMessage("Completing...")
                        progressDialog?.setCancelable(false)
                        progressDialog?.show()
                        val position = viewHolder.adapterPosition
                        val deletedGoal = adapter.getGoalAt(position)
                        val url = getString(R.string.url_server) + getString(R.string.url_markAsCompleted_goal) + "?goalId=" + deletedGoal.goalId
                        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                            { response ->
                                // Handle successful deletion response, if required
                                Toast.makeText(requireContext(), "You have completed a goal, Congrats!", Toast.LENGTH_SHORT).show()
                            },
                            { error ->
                                // Handle error response, if required
                                Log.d("Cannot complete this goal", "Error Response: ${error.message}")
                            }
                        )
                        requestQueue.add(jsonObjectRequest)
                        progressDialog?.dismiss()
                        loadGoal(adapter)
                        pieChart()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        dialog.dismiss()
                        adapter.notifyDataSetChanged()
                    }
                val alert = builder.create()
                alert.show()
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
                        requireContext(), R.color.btnColor
                    )
                ).addActionIcon(R.drawable.baseline_done_24).create().decorate()
                super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                val goal = adapter.getGoalAt(position)
                return if (goal.completedDate == null) {
                    super.getSwipeDirs(recyclerView, viewHolder) // Allow swipe
                } else {
                    Toast.makeText(requireContext(), "This goal has already completed", Toast.LENGTH_SHORT).show()
                    0 // Disable swipe
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.goalRecyclerview)
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

    private fun delete(adapter: GoalAdapter) {
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
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to Delete?").setCancelable(false)
                    .setPositiveButton("Delete") { dialog, id ->
                        val position = viewHolder.adapterPosition
                        val deletedGoal = adapter.getGoalAt(position)
                        if(deletedGoal.completedDate != null){
                            deleteIngredient(deletedGoal.goalId)
                        }
                        val url = getString(R.string.url_server) + getString(R.string.url_delete_goal) + "?goalId=" + deletedGoal.goalId
                        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
                            { response ->
                                // Handle successful deletion response, if required
//                                Toast.makeText(requireContext(), "Goal is deleted successfully.", Toast.LENGTH_SHORT).show()
                                if(deletedGoal.completedDate == null){
                                    clearGoalIdForIngredient(deletedGoal.goalId)
                                }

                            },
                            { error ->
                                // Handle error response, if required
                                Log.d("Errorrrrrr", "Error Response: ${error.message}")
                            }
                        )
                        requestQueue.add(jsonObjectRequest)
                        progressDialog?.dismiss()
                        adapter.notifyDataSetChanged()
                        loadGoal(adapter)
                        val mainActivity = activity as? MainActivity
                        mainActivity?.loadIngredient()
                    }.setNegativeButton("Cancel") { dialog, id ->
                        dialog.dismiss()
                        adapter.notifyDataSetChanged()
                    }
                val alert = builder.create()
                alert.show()
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
        itemTouchHelper.attachToRecyclerView(binding.goalRecyclerview)
    }

    private fun deleteIngredient(goalId: Int) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Deleting...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        val url = getString(R.string.url_server) + getString(R.string.url_deleteIngredientWithGoalId) + "?goalId=" + goalId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                // Handle successful deletion response, if required
                progressDialog?.dismiss()

            },
            { error ->
                // Handle error response, if required
                Log.d("FK", "Error Response: ${error.message}")
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun clearGoalIdForIngredient(goalId: Int) {
        val url = getString(R.string.url_server) + getString(R.string.url_clearGoalIdForIngredient_goal) + "?goalId=" + goalId
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                Toast.makeText(requireContext(), "Goal is successfully deleted.", Toast.LENGTH_SHORT).show()
            },
            { error ->
                // Handle error response, if required
                Log.d("FK", "Error Response: ${error.message}")
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    private fun loadGoal(adapter: GoalAdapter) {
        pieChart()
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Loading...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_goal) + "?userId=${auth.currentUser?.uid}"
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        if (goalViewModel.goalList.value?.isNotEmpty()!!) {
                            goalViewModel.deleteAllGoals()
                        }

                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonGoal: JSONObject = jsonArray.getJSONObject(i)
                                val goalId = jsonGoal.getInt("goalId")
                                val goalName = jsonGoal.getString("goalName")
                                val targetCompletionDateString = jsonGoal.getString("targetCompletionDate")
                                val targetCompletionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(targetCompletionDateString)
                                val targetCompletionDateInMillis = targetCompletionDate?.time ?: 0L
                                val dateCreatedString = jsonGoal.getString("dateCreated")

                                val dateCreated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateCreatedString)
                                val dateCreatedInMillis = dateCreated?.time ?: 0L
                                val completedDateString = jsonGoal.optString("completedDate")
                                val completedDate = try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .parse(completedDateString)
                                } catch (e: Exception) {
                                    null
                                }
                                val completedDateInMillis = completedDate?.time ?: 0L

                                val uncompletedDateString = jsonGoal.optString("uncompletedDate")
                                val uncompletedDate = try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .parse(uncompletedDateString)
                                } catch (e: Exception) {
                                    null
                                }
                                val uncompletedDateInMillis = uncompletedDate?.time ?: 0L

                                val userId = jsonGoal.getString("userId")

                                val goal = Goal(
                                    goalId,
                                    goalName,
                                    Date(targetCompletionDateInMillis),
                                    Date(dateCreatedInMillis),
                                    if (completedDate != null) Date(completedDateInMillis) else null,
                                    if (uncompletedDate != null) Date(uncompletedDateInMillis) else null,
                                    userId
                                )
                                Log.d("GoalObj", goal.toString())
                                goalViewModel.addGoal(goal)
                            }
                        }

                        // Dismiss the progress dialog when finished loading ingredients
                        progressDialog?.dismiss()
                        binding.emptyHereContent.visibility  = View.GONE
                        binding.goalSearchView.visibility  = View.VISIBLE
                        binding.goalRecyclerview.visibility  = View.VISIBLE
                        binding.allGoalsTextView.visibility  = View.VISIBLE
                        binding.goalRecyclerview.visibility  = View.VISIBLE
                        binding.goalStatistics.visibility  = View.VISIBLE
                        binding.pieChart.visibility  = View.VISIBLE
                        binding.legend.visibility  = View.VISIBLE
                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                    progressDialog?.dismiss()
                } catch (e: Exception) {
                    Log.d("ContactRepository", "Response: ${e.message}")
                    progressDialog?.dismiss()
                }
            },
            { error ->
                goalViewModel.deleteAllGoals()
                binding.emptyHereContent.visibility  = View.VISIBLE
                binding.goalSearchView.visibility  = View.GONE
                binding.goalRecyclerview.visibility  = View.GONE
                binding.allGoalsTextView.visibility  = View.GONE
                binding.goalRecyclerview.visibility  = View.GONE
                binding.goalStatistics.visibility  = View.GONE
                binding.pieChart.visibility  = View.GONE
                binding.legend.visibility  = View.GONE
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

    private fun pieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)

        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.setOnChartValueSelectedListener(this)


        pieChart.holeRadius = 0f
        pieChart.transparentCircleRadius = 5f

        pieChart.setDrawCenterText(true)
        pieChart.centerText = ""
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = false
        pieChart.isHighlightPerTapEnabled = true

        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)

        val currentDate = Calendar.getInstance().time

        val expiredGoals = goalViewModel.goalList.value?.count {
            logg("ExpireGoalDateCompare: $currentDate , ${it.targetCompletionDate}")
            it.completedDate == null && currentDate.after(it.targetCompletionDate)
        } ?: 0

        val pendingGoals = goalViewModel.goalList.value?.count {
            logg("PendingGoalDateCompare: $currentDate , ${it.targetCompletionDate}")
            it.completedDate == null && currentDate.before(it.targetCompletionDate)
        } ?: 0

        val completedGoals = goalViewModel.goalList.value?.count {
            it.completedDate != null
        } ?: 0

        logg("Complete: $completedGoals Expired: $expiredGoals Pending: $pendingGoals")

        val entries: ArrayList<PieEntry> = ArrayList()
        val colors: ArrayList<Int> = ArrayList()

        if (pendingGoals > 0) {
            entries.add(PieEntry(pendingGoals.toFloat(), "Active"))
            colors.add(ContextCompat.getColor(requireContext(), R.color.textColor))
        }
        if (completedGoals > 0) {
            entries.add(PieEntry(completedGoals.toFloat(), "Completed"))
            colors.add(ContextCompat.getColor(requireContext(), R.color.btnColor))
        }
        if (expiredGoals > 0) {
            entries.add(PieEntry(expiredGoals.toFloat(), "Uncompleted"))
            colors.add(ContextCompat.getColor(requireContext(), R.color.secondaryColor))
        }

        logg("entries: $entries")

        val dataSet = PieDataSet(entries, "Goals")

        logg("dataset: $dataSet")
        dataSet.colors = colors

        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0f, 40f)
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(15f)
        data.setValueTypeface(Typeface.DEFAULT_BOLD)
        data.setValueTextColor(Color.WHITE)
        pieChart.data = data

        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun pieChartAnimation() {
        // Animate the pie chart after data is set
        pieChart.animateY(1000, Easing.EaseInOutQuad)
    }


    override fun onGoalClick(goal: Goal) {
        findNavController().navigate(R.id.action_goalFragment_to_goalDetailFragment)
        setFragmentResult("requestName", bundleOf("name" to goal.goalName))
        setFragmentResult(
            "requestDate", bundleOf(
                "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(goal.targetCompletionDate)
            )
        )
        setFragmentResult("requestId", bundleOf("id" to goal.goalId))
        Log.d("goalIdFragmentResult", goal.goalId.toString())
        //Log.d("imageIngredient", ingredient.ingredientImage.toString())
        //setFragmentResult("requestImage", bundleOf("image" to ingredient.ingredientImage))
        disableBtmNav()
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        // e is the selected PieEntry
        val selectedSliceLabel = (e as? PieEntry)?.data?.toString() ?: ""

        // Get the number of goals for the selected slice
        val numGoals = when (selectedSliceLabel) {
            "Active" -> goalViewModel.goalList.value?.count {
                it.completedDate == null && it.uncompletedDate == null
            }
            "Completed" -> goalViewModel.goalList.value?.count {
                it.completedDate != null && it.uncompletedDate == null
            }
            "Uncompleted" -> goalViewModel.goalList.value?.count {
                it.completedDate == null && it.uncompletedDate != null
            }
            else -> 0 // Default value if the label doesn't match any of the expected values
        }

        // Update the center text of the PieChart with the number of goals
        if (numGoals != null) {
            if (numGoals > 0) {
                pieChart.centerText = "$numGoals\nGoals"
                pieChart.invalidate()
            }
        }
    }

    override fun onNothingSelected() {
        // This method is called when no slice is selected.
        // You can perform any additional actions here if needed.
    }

    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun logg(msg:String){
        Log.d("GoalFragment", msg)
    }

}
