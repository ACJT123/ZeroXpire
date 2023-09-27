package my.edu.tarc.zeroxpire.view.profile

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentProfileBinding
import my.edu.tarc.zeroxpire.model.IngredientDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var auth: FirebaseAuth
    private var profilePictureUrl: String? = ""
    private var username: String? = ""

    private val ingredientDatabase by lazy {
        IngredientDatabase.getDatabase(requireContext()).ingredientDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        getUsername()
        if(auth.currentUser != null){
            val user = Firebase.auth.currentUser
            val photoUrl = user?.photoUrl
            profilePictureUrl = photoUrl?.toString()
            if (profilePictureUrl != null) {
                Glide.with(this)
                    .load(profilePictureUrl)
                    .into(binding.profilePicture)
            }
            else{
                Glide.with(this)
                    .load(R.drawable.user)
                    .into(binding.profilePicture)
            }
            username = user?.displayName
            binding.username.text = username.toString()
            val email = user?.email
            binding.email.text = email.toString()
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.termsAndConditionsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_termsAndConditionsFragment)
            disableBtmNav()
        }

        binding.notificationsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_notificationsFragment)
            disableBtmNav()
        }

//        binding.editProfileCard.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
//            setFragmentResult("requestName", bundleOf("name" to username))
//            setFragmentResult("requestProfilePic", bundleOf("profilePic" to profilePictureUrl))
//            disableBtmNav()
//        }

        binding.logoutBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to Logout?")
                .setCancelable(false)
                .setPositiveButton("Logout") { dialog, id ->
                    logout()
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        binding.deleteAccBtn.setOnClickListener {
            logg("currentuser:  ${auth.currentUser?.uid.toString()}")
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to Delete?")
                .setCancelable(false)
                .setPositiveButton("Delete") { dialog, id ->
                    deleteAcc()
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        navigateBack()
    }

    private fun getUsername(){
        val url: String = getString(R.string.url_server) + getString(R.string.url_read_username) + "?userId=${auth.currentUser?.uid}"
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


                        if (size > 0) {
                            for (i in 0 until size) {
                                val jsonUser: JSONObject = jsonArray.getJSONObject(i)
                                val getUserId = jsonUser.getString("userId")
                                val getUserName = jsonUser.getString("userName")
                                val stayLoggedIn = jsonUser.getInt("stayLoggedIn")
                                Log.d("username", getUserName)
                                binding.username.text = getUserName
                            }
                        }


                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")

                } catch (e: java.lang.Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")

                }
            },
            { error ->
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
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

    private fun deleteAcc() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Deleting...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
        val url = getString(R.string.url_server) + getString(R.string.url_delete_user) + "?userId=" + auth.currentUser?.uid
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {
                            Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
                                if(task.isSuccessful){
                                    findNavController().navigate(R.id.loginFragment)
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        ingredientDatabase.deleteAllIngredient()
                                    }


                                    //clear all shared preferences
                                    val sharedPreferences = requireActivity().getSharedPreferences("sharedPreference", 0)
                                    val editor = sharedPreferences.edit()
                                    editor.clear()
                                    editor.apply()


                                    disableBtmNav()
                                    Toast.makeText(requireContext(), "Account is deleted successfully", Toast.LENGTH_SHORT).show()
                                    progressDialog.dismiss()
                                } else{
                                    Toast.makeText(requireContext(), task.exception.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("AddIngredientFragment", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->
                Log.d("AddIngredientFragmentID", "Response : %s".format(error.message.toString()))
            }
        )

        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }

    private fun disableBtmNav(){
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }

    private fun logout(){
        Firebase.auth.signOut()
        disableBtmNav()
        findNavController().navigate(R.id.loginFragment)

    }

    private fun logg(msg: String){
        Log.d("ProfileFragment:",  "$msg")
    }


}