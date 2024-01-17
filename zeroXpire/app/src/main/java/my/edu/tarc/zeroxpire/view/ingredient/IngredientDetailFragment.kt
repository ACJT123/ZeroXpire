package my.edu.tarc.zeroxpire.view.ingredient

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentIngredientDetailBinding
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.viewmodel.IngredientViewModel
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class IngredientDetailFragment : Fragment() {
    private lateinit var binding: FragmentIngredientDetailBinding

    private lateinit var auth: FirebaseAuth
    private var selectedDate: Long? = null // Variable to store the selected date as a Long value

    private val ingredientViewModel: IngredientViewModel by activityViewModels()

    private var ingredientId: Int = 0
    private var originalName: String? = null
    private var originalSelectedDate: Long? = null
    private var originalImage: Uri? = null

    private lateinit var requestQueue: RequestQueue

    private var imageFile: Uri? = null

    private var progressDialog: ProgressDialog? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIngredientDetailBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set fragment result listeners to receive data from other fragments
        setFragmentResultListener("requestName") { _, bundle ->
            val result = bundle.getString("name")
            originalName = result
            binding.enterIngredientName.setText(originalName)
        }

        setFragmentResultListener("requestDate") { _, bundle ->
            val dateString = bundle.getString("date")
            selectedDate = parseDateStringToLong(dateString)
            originalSelectedDate = parseDateStringToLong(dateString)
            binding.chooseExpiryDate.setText(dateString)
        }

        setFragmentResultListener("requestCategory") { _, bundle ->
            val selectedCategory = bundle.getString("category")

            // Find the position of the selected category in the list
            val ingredientCategories = resources.getStringArray(R.array.ingredient_categories)
            val categoryPosition = ingredientCategories.indexOf(selectedCategory)

            // Set the selected category's position in the AutoCompleteTextView
            val adapter =
                ArrayAdapter(requireContext(), R.layout.category_list, ingredientCategories)
            binding.chooseCategory.setAdapter(adapter)

            //TODO
            //binding.chooseCategory.setSelection(categoryPosition)

            Log.d("IngredientDetailFragment", "Selected category position: $categoryPosition")
        }


        setFragmentResultListener("requestId") { _, bundle ->
            val id = bundle.getInt(
                "id",
                0
            ) // Use a default value if the key is not found or the value is not an integer
            ingredientId = id
        }

        setFragmentResultListener("requestName") { _, bundle ->
            val result = bundle.getString("name")
            originalName = result
            binding.enterIngredientName.setText(originalName)
        }

        binding.ingredientImage.setPadding(0, 0, 0, 0)
        binding.ingredientImage.scaleType = ImageView.ScaleType.CENTER_CROP
        var image: String? = ""
        setFragmentResultListener("requestImage") { _, bundle ->
            val imageUri = bundle.getString("image")
            image = imageUri
            originalImage = imageUri?.toUri()
            Glide.with(requireContext())
                .load(imageUri)
                .centerCrop()
                .into(binding.ingredientImage)
        }

        showCategory()

        // Button click listeners
        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.chooseExpiryDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveBtn.setOnClickListener {
            val newName = binding.enterIngredientName.text.toString()
            val newDate = selectedDate
            val newExpiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(newDate)

            if (newName.isNotEmpty()) {
                progressDialog = ProgressDialog(requireContext())
                progressDialog?.setMessage("Updating...")
                progressDialog?.setCancelable(false)
                progressDialog?.show()
                //todo: only workable if change all 3 fields


                val storage = Firebase.storage("gs://zeroxpire.appspot.com")
                val encodedIngredientImage = URLEncoder.encode(
                    originalName + auth.currentUser?.uid,
                    "UTF-8"
                )
                val imageRef =
                    storage.reference.child("ingredientImage/${encodedIngredientImage}.jpg")
                Log.d("imageRef", imageRef.toString())

                if(imageFile == null){
                    storeIngredientToDB(newName, newExpiryDate, image!!)
                }
                else{
                    imageFile?.let { uri ->
                        //delete current one
//                        imageRef.delete().addOnSuccessListener {
                        val newIngredientImage = URLEncoder.encode(
                            newName + auth.currentUser?.uid,
                            "UTF-8"
                        )
                        val newImageRef =
                            storage.reference.child("ingredientImage/${newIngredientImage}.jpg")
                        newImageRef.putFile(uri).addOnSuccessListener {
                            newImageRef.downloadUrl.addOnSuccessListener { downloadedUri ->
                                logg("downloadedUri: $downloadedUri")
                                val imageUrl = downloadedUri.toString()
                                storeIngredientToDB(newName, newExpiryDate, imageUrl)
                            }
                        }


//                            progressDialog!!.dismiss()
//                            findNavController().popBackStack()

//                        }.addOnFailureListener {
//                            logg("cannot delete")
//                        }
                    }
                }

            } else {
                if (newName.isEmpty()) {
                    binding.enterIngredientName.error = "Please enter the ingredient's name"
                    binding.enterIngredientName.requestFocus()
                }
                if (!checkExist(newName)) {
                    binding.enterIngredientName.error =
                        "Ingredient has already exists, choose other name."
                    binding.enterIngredientName.requestFocus()
                }


                return@setOnClickListener
            }
        }

        binding.deleteBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.ingredientImage.setPadding(0, 0, 0, 0)
        binding.ingredientImage.scaleType = ImageView.ScaleType.CENTER_CROP
        imageFile = arguments?.getParcelable<Uri>("ingredientImage")
        Log.d("ingredientImage!!", imageFile.toString())
        Glide.with(requireContext())
            .load(imageFile)
            .centerCrop()
            .into(binding.ingredientImage)

        binding.ingredientImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
    }

    private fun checkExist(newName: String): Boolean {
        val ingredientNameList = mutableListOf<Ingredient>()

        ingredientViewModel.ingredientList.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { ingredients ->
                ingredients.map {
                    if (it.ingredientName == newName) {
                        ingredientNameList.add(it)
                    }
                }
            })

        return ingredientNameList.size == 0

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            imageFile = data.data
            Glide.with(requireContext())
                .load(imageFile)
                .centerCrop()
                .into(binding.ingredientImage)
        }
    }

    private fun showCategory() {
        val ingredientCategories = resources.getStringArray(R.array.ingredient_categories)
        val adapter = ArrayAdapter(requireContext(), R.layout.category_list, ingredientCategories)
        binding.chooseCategory.setAdapter(adapter)

        var category: String? = null
        var index: Int? = null
        ingredientViewModel.ingredientList.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { ingredients ->
                ingredients.map {
                    if (ingredientId == it.ingredientId) {
                        category = it.ingredientCategory
                    }
                }
                logg("category: $category")

                // Find the index of the preselected category in the ingredientCategories array
                val preselectedIndex = ingredientCategories.indexOf(category)
                index = preselectedIndex
                logg("preselectedIndex: $preselectedIndex")
                if (preselectedIndex != -1) {
                    // Set the selection programmatically
                    binding.chooseCategory.setText(ingredientCategories[preselectedIndex], false)
                    index = preselectedIndex
                }
            })

        // Set an item click listener to handle the selected category
        binding.chooseCategory.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedCategory = ingredientCategories[position]
            }

        // Programmatically open the dropdown list when the user clicks the AutoCompleteTextView
        binding.chooseCategory.setOnClickListener {
            binding.chooseCategory.showDropDown()
        }
    }


    private fun storeIngredientToDB(newName: String, newDate: String, imageUrl: String) {
        val url =
            "https:/zeroxpire.000webhostapp.com/api/ingredient/update.php" + "?ingredientName=" + newName +
                    "&expiryDate=" + newDate + "&ingredientImage=" + URLEncoder.encode(
                imageUrl.substringAfterLast("%2F"), "UTF-8"
            ) +
                    "&ingredientCategory=" + binding.chooseCategory.text.toString() +
                    "&ingredientId=" + ingredientId
        logg("newImageUrl: $url")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {

                            progressDialog!!.dismiss()
                            findNavController().popBackStack()
                            logg("Ingredient is updated successfully.")
                            Toast.makeText(requireContext(), "Ingredient is updated successfully", Toast.LENGTH_SHORT).show()

                        } else {
                            logg("Failed to update.")
                        }
                    }
                } catch (e: java.lang.Exception) {
                    Log.d("Update", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->
                // Handle error response, if required
                Log.d("Update", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy =
            DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(binding.root.context).addToRequestQueue(jsonObjectRequest)

    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = selectedCalendar.timeInMillis
                val dateString = formatDateToStringFromLong(selectedDate)
                binding.chooseExpiryDate.setText(dateString)
            },
            year,
            month,
            dayOfMonth
        )

        // Set the selected date as the default date
        if (selectedDate != null) {
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.timeInMillis = selectedDate!!

            val selectedYear = selectedCalendar.get(Calendar.YEAR)
            val selectedMonth = selectedCalendar.get(Calendar.MONTH)
            val selectedDayOfMonth = selectedCalendar.get(Calendar.DAY_OF_MONTH)

            datePickerDialog.updateDate(selectedYear, selectedMonth, selectedDayOfMonth)
        }

        datePickerDialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Are you sure you want to delete?")
            .setCancelable(false)
            .setPositiveButton("Delete") { _, _ ->
                val url =
                    getString(R.string.url_server) + getString(R.string.url_delete_ingredient) + "?ingredientId=" + ingredientId
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.POST, url, null,
                    { response ->
                        try {
                            if (response != null) {
                                val strResponse = response.toString()
                                val jsonResponse = JSONObject(strResponse)
                                val success: String = jsonResponse.get("success").toString()

                                if (success == "1") {
                                    Toast.makeText(
                                        requireContext(),
                                        "Ingredient is deleted successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Fail to delete.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: java.lang.Exception) {
                            Log.d("Delete", "Response: %s".format(e.message.toString()))
                        }
                    },
                    { error ->
                        // Handle error response, if required
                        Log.d("Delete", "Error Response: ${error.message}")
                    }
                )
                jsonObjectRequest.retryPolicy =
                    DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
                WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun parseDateStringToLong(dateString: String?): Long? {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = format.parse(dateString)
            date?.time
        } catch (e: Exception) {
            null
        }
    }


    private fun formatDateToStringFromLong(dateLong: Long?): String {
        return if (dateLong != null) {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.format(Date(dateLong))
        } else {
            ""
        }
    }


    private fun logg(msg: String) {
        Log.d("ingredientDetailFragment", msg)
    }
}