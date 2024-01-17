package my.edu.tarc.zeroxpire.view.profile

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentLoginBinding
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.UnknownHostException
import java.util.*

class LoginFragment : Fragment() {


    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    val Req_Code: Int = 123
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var requestQueue: RequestQueue

    private val sharedPrefFile = "sharedpreference"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        FirebaseApp.initializeApp(requireContext())

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("856335947735-af79prie6ai6heutkh8ekl40u1epasn2.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        auth = FirebaseAuth.getInstance()

        binding.loginWithGoogleBtn.setOnClickListener {
            loginWithGoogle()
        }
        requestQueue = Volley.newRequestQueue(requireContext())
        return binding.root
    }

    private fun loginWithGoogle() {

        googleSignInClient.signOut()
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResult(task)
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResult(task)
            }
        }

    private fun handleResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Logging In...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }

//                toast(account.email.toString())
                createUser(auth.currentUser)
                setFragmentResult("requestEmail", bundleOf("email" to account.email))

                findNavController().navigate(R.id.ingredientFragment)
                findNavController().clearBackStack(R.id.ingredientFragment)
            } else {
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
                Toast.makeText(requireContext(), task.exception.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if the user was previously logged in and chose to stay logged in
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getInt("stay_logged_in", 0) == 1

        if (isLoggedIn) {
            // Navigate to the appropriate screen (ingredientFragment in this case)
            findNavController().navigate(R.id.ingredientFragment)
            findNavController().clearBackStack(R.id.ingredientFragment)
            enableBtmNav()
        }

        binding.createAccBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.forgotPasswordBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.loginBtn.setOnClickListener {
            normalLogin()
        }

        navigateBackListeners()

    }

    private fun navigateBackListeners() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("Are you sure you want to Exit?").setCancelable(false)
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

    private fun createUser(account: FirebaseUser?) {

        val url = getString(R.string.url_server) + getString(R.string.url_create_user) +
                "?userId=" + account?.uid +
                "&userName=" + account?.displayName +
                "&stayLoggedIn=" + 1

        Log.d("url", url)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val success: String = jsonResponse.get("success").toString()

                        if (success == "1") {
                            Toast.makeText(
                                requireContext(),
                                "User has created successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to login",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("Cannot login", "Response: %s".format(e.message.toString()))
                }
            },
            { error ->
                Log.d("Cannot login .....", "Response : %s".format(error.message.toString()))
            }
        )
        jsonObjectRequest.retryPolicy =
            DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 1f)
        WebDB.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }

    private fun navigateBack() {
        findNavController().navigate(R.id.ingredientFragment)
        findNavController().clearBackStack(R.id.ingredientFragment)
    }


    private fun normalLogin() {
        val email = binding.enterEmail.text.toString()
        val password = binding.enterPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            val progressDialog = ProgressDialog(requireContext())
            progressDialog.setMessage("Logging In...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (progressDialog.isShowing) {
                            progressDialog.dismiss()
                            findNavController().navigate(R.id.ingredientFragment)
                            findNavController().clearBackStack(R.id.ingredientFragment)
                            enableBtmNav()
                        }
                    } else {
                        // If sign in fails, display error message to the user.
                        Toast.makeText(
                            requireContext(),
                            task.exception?.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            if (email.isEmpty()) {
                binding.enterEmail.error = "Please enter the email address."
                binding.enterEmail.requestFocus()
            }
            if (password.isEmpty()) {
                binding.enterPasswordLayout.endIconMode = TextInputLayout.END_ICON_NONE
                binding.enterPassword.error = "Please enter the password."
                binding.enterPassword.requestFocus()

                // Enable back when the text field is filled
                binding.enterPassword.doAfterTextChanged {
                    binding.enterPasswordLayout.endIconMode =
                        TextInputLayout.END_ICON_PASSWORD_TOGGLE
                }
            }
        }
    }


    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }


    private fun enableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.VISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.VISIBLE
    }
}