package my.edu.tarc.zeroxpire

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import my.edu.tarc.zeroxpire.adapters.IngredientAdapter
import my.edu.tarc.zeroxpire.adapters.RecognitionResultsAdapterDate
import my.edu.tarc.zeroxpire.adapters.RecognitionResultsAdapterName
import my.edu.tarc.zeroxpire.databinding.ActivityMainBinding
import my.edu.tarc.zeroxpire.ingredient.IngredientClickListener
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.GoalViewModel
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), IngredientClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    private lateinit var ingredientViewModel: IngredientViewModel
    private lateinit var goalViewModel: GoalViewModel

    private lateinit var ingredientAdapter: IngredientAdapter

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetView: View
    private lateinit var recyclerView: RecyclerView

    private val selectedIngredients: MutableList<Ingredient> = mutableListOf()

    private val selectedRecognizedName: MutableList<String> = mutableListOf()

    //text recog
    private var imageUri: Uri? = null
    private var imageIngredientNameUri: Uri? = null
    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>
    private lateinit var progressDialog: ProgressDialog
    private lateinit var textRecognizer: TextRecognizer

    private var recognizedIngredientName: String? = null
    private var recognizedExpiryDates: String? = null

    private val cameraRequestCode = 1000
    private val notificationRequestCode = 1000

    private var hasGetName = false;
    private var hasGetDate = false;

    private var isManualOptionChosen = false

    // Define a variable to keep track of the count
    private val nearlyExpiredIngredients: MutableList<Ingredient> = mutableListOf()

    // declaring variables
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private val channelId = "i.apps.notifications"
    private val description = "Test notification"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraRequestCode
            )
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationRequestCode
            )
        }

//        createNotificationChannel()

        ingredientViewModel = ViewModelProvider(this)[IngredientViewModel::class.java]

        // create shared preference for storing reminterTime value
        val sharedPref = getSharedPreferences("reminderTime", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("reminderTime", 3)
        editor.apply()


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //initialize ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)
        goalViewModel = ViewModelProvider(this).get(GoalViewModel::class.java)

        // Initialize IngredientAdapter
        ingredientAdapter = IngredientAdapter(this, goalViewModel)
        // Navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        // Find reference to bottom navigation view
        val navView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        // Hook your navigation controller to bottom navigation view
        navView.setupWithNavController(navController)

        navView.background = null
        navView.menu.getItem(2).isEnabled = false

        disableBtmNav()

        if (auth.currentUser != null) {
            navController.navigate(R.id.ingredientFragment)
            navController.clearBackStack(R.id.ingredientFragment)
            loadIngredient()
            enableBtmNav()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.ingredientFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.barcode_scan_icon_137911)
                    binding.fab.setOnClickListener {
                        bottomSheetDialog = BottomSheetDialog(this)
                        bottomSheetView = layoutInflater.inflate(
                            R.layout.bottom_sheet_add_ingredient_option,
                            null
                        )
                        bottomSheetDialog.setContentView(bottomSheetView)
                        bottomSheetDialog.show()
                        val scanner = bottomSheetView.findViewById<MaterialCardView>(R.id.scannerOption)
                        val manual = bottomSheetView.findViewById<MaterialCardView>(R.id.manualOption)

                        scanner.setOnClickListener {
                            byRecognition()
                            bottomSheetDialog.dismiss()
                            disableBtmNav()
                        }

                        manual.setOnClickListener {
                            bottomSheetDialog.dismiss()
                            disableBtmNav() // Disable the bottom navigation view when manual option is chosen
                            isManualOptionChosen = true // Set the flag to true when manual option is chosen
                            navController.navigate(R.id.action_ingredientFragment_to_addIngredientFragment)
                        }


                        bottomSheetDialog.setOnDismissListener {
                            if (!isManualOptionChosen) {
                                enableBtmNav() // Re-enable the bottom navigation view when the bottom sheet is dismissed
                            }
                        }
                    }
                    binding.fab.isEnabled = true
                }
                R.id.goalFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.baseline_add_24)
                    binding.fab.setOnClickListener {
//                        showBottomSheetDialog(ingredientAdapter)
                        navController.navigate(R.id.action_goalFragment_to_createGoalFragment)
                        disableBtmNav()
                    }
                    binding.fab.isEnabled = ingredientViewModel.ingredientList.value?.size != 0
                }
                R.id.recipeFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.baseline_book_24)
                    binding.fab.setOnClickListener {
                        navController.navigate(R.id.action_recipeFragment_to_recipeCreateFragment)
                        disableBtmNav()
                    }
                    binding.fab.isEnabled = ingredientViewModel.ingredientList.value?.size != 0
                }
                R.id.profileFragment -> {
                    enableBtmNav()
                    binding.fab.setImageResource(R.drawable.barcode_scan_icon_137911)
                    binding.fab.setOnClickListener {
                        bottomSheetDialog = BottomSheetDialog(this)
                        bottomSheetView = layoutInflater.inflate(
                            R.layout.bottom_sheet_add_ingredient_option,
                            null
                        )
                        bottomSheetDialog.setContentView(bottomSheetView)
                        bottomSheetDialog.show()
                        val scanner = bottomSheetView.findViewById<MaterialCardView>(R.id.scannerOption)
                        val manual = bottomSheetView.findViewById<MaterialCardView>(R.id.manualOption)

                        scanner.setOnClickListener {
                            byRecognition()
                            bottomSheetDialog.dismiss()
                            disableBtmNav()
                        }

                        manual.setOnClickListener {
                            bottomSheetDialog.dismiss()
                            disableBtmNav() // Disable the bottom navigation view when manual option is chosen
                            isManualOptionChosen = true // Set the flag to true when manual option is chosen
                            navController.navigate(R.id.action_profileFragment_to_addIngredientFragment)
                        }


                        bottomSheetDialog.setOnDismissListener {
                            if (!isManualOptionChosen) {
                                enableBtmNav() // Re-enable the bottom navigation view when the bottom sheet is dismissed
                            }
                        }
                    }
                    binding.fab.isEnabled = true
                }
            }
        }
    }

    override fun onBackPressed() {
        // Check if the camera option was chosen
        if (isManualOptionChosen) {
            enableBtmNav() // Re-enable the bottom navigation view
            isManualOptionChosen = false // Reset the flag
        }

        super.onBackPressed()
    }

    private fun byRecognition() {
        //text recog permissions
        cameraPermissions =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraActivityResultLauncher.launch(intent)
    }

    private fun byRecognitionDate() {
        imageUri = null
        //text recog permissions
        cameraPermissions =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        cameraActivityResultLauncherDate.launch(intent)
    }


    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startRecognizeName()
            }
        }

    private val cameraActivityResultLauncherDate =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startRecognizeDate()
            }
        }


    private fun startRecognizeName() {
        progressDialog.setMessage("Processing image")
        progressDialog.show()
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            progressDialog.setMessage("Recognizing text")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    progressDialog.dismiss()

                    // Split the recognized text into separate lines or words
                    val lines = text.text.split("\n")
                    Log.d("line", lines.toString())

                    // Display the recognized lines/words to the user for selection
                    displayRecognitionResultsName(lines)

                }
        } catch (e: Exception) {
            toast("Fail to capture the image")
            progressDialog.dismiss()
        }
    }



    private fun displayRecognitionResultsName(results: List<String>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_recognition_results, null)
        dialog.setContentView(view)
        dialog.show()
        dialog.setCancelable(false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecognitionResults)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val mergeTextField = view.findViewById<TextView>(R.id.mergedTextView)

        val adapter = RecognitionResultsAdapterName(this, results) { selectedResult ->
            if (selectedRecognizedName.contains(selectedResult)) {
                selectedRecognizedName.remove(selectedResult)
            } else {
                selectedRecognizedName.add(selectedResult)
            }

            recognizedIngredientName = selectedRecognizedName.joinToString(separator = " ")
            mergeTextField.text = recognizedIngredientName.toString()
        }

        val rescanBtn = view.findViewById<Button>(R.id.rescanBtn)
        rescanBtn.setOnClickListener {
            dialog.dismiss()
            adapter.selectedItems.clear()
            byRecognition()
        }





        view.findViewById<TextView>(R.id.textView).text = "${adapter.itemCount} possible names found:"
        recyclerView.adapter = adapter

        val cancelBtn = view.findViewById<ImageView>(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }


        val continueBtn = view.findViewById<Button>(R.id.continueBtn)
        continueBtn.setOnClickListener {
            imageIngredientNameUri = imageUri
//            val combinedRecognizedNames = selectedRecognizedName.joinToString(separator = " ")
//            recognizedIngredientName = combinedRecognizedName
            Log.d("recooogName", recognizedIngredientName.toString())
            dialog.dismiss()

            // Now, initiate the date recognition process
            byRecognitionDate()
        }
    }


    private fun startRecognizeDate() {
        progressDialog.setMessage("Processing image")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            progressDialog.setMessage("Recognizing text")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    progressDialog.dismiss()

                    try {
                        val dates = findAllDatesInText(text.text)
                        Log.d("dates", dates.toString())

                        if (dates.isNotEmpty()) {
                            recognizedExpiryDates = dates.joinToString(separator = "\n")
                            Log.d("recooogDate", recognizedExpiryDates.toString())

                            // Display the bottom sheet for date selection
                            displayRecognitionResultsDate(recognizedExpiryDates!!)
                        } else {
                            // Handle the case when no dates are found
                            progressDialog.dismiss()
                            byRecognitionDate()
                            toast("try again")
                        }
                    } catch (e: Exception) {
                        Log.d("unparsable", e.toString())
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    byRecognitionDate()
                    // Handle failure
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            byRecognitionDate()
            // Handle the exception
        }
    }



    private fun displayRecognitionResultsDate(results: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_recognition_results, null)
        dialog.setContentView(view)
        dialog.show()
        dialog.setCancelable(false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecognitionResults)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val adapter = RecognitionResultsAdapterDate(this,results.split("\n")) { selectedResult ->
            // Handle the selected date here
            // For example, you can assign the selectedResult to recognizedExpiryDates
            recognizedExpiryDates = selectedResult
            Log.d("recoggDateResult", recognizedExpiryDates.toString())
        }

        view.findViewById<TextView>(R.id.textView).text = "${adapter.itemCount} possible dates found:"
        recyclerView.adapter = adapter

        val cancelBtn = view.findViewById<ImageView>(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        val mergedTextView = view.findViewById<TextView>(R.id.mergedTextView)
        mergedTextView.visibility = View.GONE

        val mergedTextViewLabel = view.findViewById<TextView>(R.id.mergedTextViewLabel)
        mergedTextViewLabel.visibility = View.GONE

        val continueBtn = view.findViewById<Button>(R.id.continueBtn)
        continueBtn.text = "complete"
        continueBtn.setOnClickListener {
            if (recognizedExpiryDates != null) {
                hasGetDate = true
                navigateToNextFragment() // Call this function to proceed after date recognition
                dialog.dismiss()
            } else {
                // Inform the user to select a date before continuing
                // You can show a Toast or set an error message
            }
        }

        val rescanBtn = view.findViewById<Button>(R.id.rescanBtn)
        rescanBtn.setOnClickListener {
            dialog.dismiss()
            byRecognitionDate()
        }

    }

    private fun navigateToNextFragment() {
        if (recognizedIngredientName != null) {
//            myFragment.arguments = bundle
//            fragmentTransaction.add(R.id.nav_host_fragment,myFragment).commit()
            val bundle = bundleOf(
                "recognizedIngredientName" to recognizedIngredientName,
                "recognizedExpiryDate" to recognizedExpiryDates,
                "ingredientImage" to imageIngredientNameUri
            )
            Log.d("bundle", bundle.toString())
            navController.navigate(R.id.action_ingredientFragment_to_addIngredientFragment, bundle)
            selectedRecognizedName.clear()
            disableBtmNav()
        } else {
            // Handle the case when either the name or the date is not recognized
            // For example, show an error message or provide feedback to the user
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release the text recognition resources
        textRecognizer.close()

        // Any other cleanup related to camera or resources
    }

    private fun findAllDatesInText(text: String): List<String> {
        val possibleDateFormats = listOf(
            SimpleDateFormat("dd.MM.yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US)
            // Add more date formats as needed
        )

        val formattedDates = mutableListOf<String>()

        for (dateFormat in possibleDateFormats) {
            val regex = "\\b\\d{2}[./-]\\d{2}[./-]\\d{4}\\b".toRegex()
            val matchResults = regex.findAll(text)

            matchResults.forEach { matchResult ->
                try {
                    val date = dateFormat.parse(matchResult.value)
                    date?.let { formattedDates.add(dateFormat.format(it)) }
                } catch (e: Exception) {
                    println("Error parsing date: ${e.message}")
                }
            }
        }

        return formattedDates
    }
    private fun disableBtmNav() {
        binding.bottomAppBar.visibility = View.INVISIBLE
        binding.fab.visibility = View.INVISIBLE
    }

    private fun enableBtmNav() {
        binding.bottomAppBar.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ResourceAsColor")
    override fun onIngredientClick(ingredient: Ingredient) {
        val layoutManager = recyclerView.layoutManager

        if (layoutManager is LinearLayoutManager) {
            val clickedItemPosition = ingredientAdapter.getPosition(ingredient)
            val clickedItemView = layoutManager.findViewByPosition(clickedItemPosition)

            val isItemSelected = clickedItemView?.tag as? Boolean ?: false

            if (isItemSelected) {
                // Reset the background color of the clicked item to the default color
                clickedItemView?.setBackgroundColor(Color.WHITE)
                clickedItemView?.tag = false

                // Deselect the ingredient if it was selected
                selectedIngredients.remove(ingredient)
            } else {
                // Change the background color of the clicked item to the selected color
                clickedItemView?.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.btnColor
                    )
                )
                clickedItemView?.tag = true

                // Select the ingredient if it was deselected
                selectedIngredients.add(ingredient)
            }
        }

        val nextBtn = bottomSheetView.findViewById<Button>(R.id.addBtn)
        nextBtn.isEnabled = selectedIngredients.isNotEmpty()

        Log.d("Ingredients", selectedIngredients.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadIngredient() {
        progressDialog = ProgressDialog(this)
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
                            var totalExpiringIngredients = 0
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
                                    //checkExpiryAndNotify(ingredient)
                                    ingredientViewModel.addIngredient(ingredient)

                                    val currentDate = Calendar.getInstance().time

                                    // Calculate days until expiry
                                    val daysUntilExpiry = ((expiryDateInMillis - currentDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1

                                    Log.d("DaysUntilExpiry", daysUntilExpiry.toString())

                                    val sharedPreference =  this.getSharedPreferences("sharedPreference", 0)
                                    val storedReminderTime = sharedPreference.getInt("reminderTime", 3)

                                    Log.d("storedReminderTime", storedReminderTime.toString())

                                    if (daysUntilExpiry <= storedReminderTime && daysUntilExpiry >= 0 && ingredient.ingredientGoalId == null) {
                                        totalExpiringIngredients++
                                    }

                                    // if (daysUntilExpiry <= 3 && daysUntilExpiry > 0) {
                                    //     totalExpiringIngredients++
                                    // }


                                    Log.d("IngredientCategory", ingredient.ingredientCategory)
                                }
                            }

                            // if the switch state is false from shared preference, then do not schedule notification
                            val sharedPreference =  this.getSharedPreferences("sharedPreference", 0)
                            val switchState = sharedPreference.getBoolean("switchState", false)

                            Log.d("swtichState", switchState.toString())
                            Log.d("totalIngredient", totalExpiringIngredients.toString())

                            if(switchState && totalExpiringIngredients > 0){
                                createNotificationChannel()
                                scheduleNotification(totalExpiringIngredients)
                            }
                        }

                        // Dismiss the progress dialog when finished loading ingredients
                        progressDialog.dismiss()

                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")

                } catch (e: Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")

                }
            },
            { error ->
                //i think is when there is nothing to return then it will return 404
                ingredientViewModel.deleteAllIngredients()

            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(this).addToRequestQueue(jsonObjectRequest)

        progressDialog.dismiss()

    }

    private fun createNotificationChannel()
    {
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleNotification(totalExpiringIngredients: Int) {
        // Get the stored daily reminder time from shared preferences
        val sharedPreference = this.getSharedPreferences("sharedPreference", 0)
        val storedReminderTime = sharedPreference.getInt("hourOfDay", 20)
        val storedReminderMinute = sharedPreference.getInt("minute", 0)
        Log.d("time", storedReminderTime.toString())
        Log.d("min", storedReminderMinute.toString())

        val reminderWithinDays = sharedPreference.getInt("reminderTime", 3)

        val intent = Intent(applicationContext, my.edu.tarc.zeroxpire.Notification::class.java)
        intent.putExtra("total", totalExpiringIngredients)
        intent.putExtra("days", reminderWithinDays)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate the time for the next day's notification
        val currentTime = Calendar.getInstance()
        val notificationTime = Calendar.getInstance()
        notificationTime.set(Calendar.HOUR_OF_DAY, storedReminderTime)
        notificationTime.set(Calendar.MINUTE, storedReminderMinute)
        notificationTime.set(Calendar.SECOND, 0)

        if (notificationTime.before(currentTime) || notificationTime == currentTime) {
            // If the desired time has already passed for the current day,
            // schedule it for the next day
            notificationTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("notifTIme", notificationTime.time.toString())

        Log.d("currentTime", System.currentTimeMillis().toString())

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            notificationTime.timeInMillis,
            pendingIntent
        )
    }

    }
    private fun logg(msg: String){
        Log.d("MainActivity:",  "$msg")
    }







