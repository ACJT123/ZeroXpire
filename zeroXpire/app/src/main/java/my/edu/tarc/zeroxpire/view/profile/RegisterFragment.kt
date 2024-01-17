package my.edu.tarc.zeroxpire.view.profile

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.databinding.FragmentRegisterBinding
import org.json.JSONObject
import java.lang.Exception

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //register -> login
        setClickListeners()

        binding.createAccBtn.setOnClickListener {
            val username = binding.enterUsername.text.toString()
            val email = binding.enterEmail.text.toString()
            val password = binding.enterPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                //validate email with regex
                val emailRegex = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}\$")
                if (!emailRegex.matches(email)) {
                    binding.enterEmail.error = "Please enter a valid email address."
                    binding.enterEmail.requestFocus()
                    return@setOnClickListener
                }
                createUserWithEmailAndPassword(email, password)
            }
            else {
                if (username.isEmpty()) {
                    binding.enterUsername.error = "Please enter the username."
                    binding.enterUsername.requestFocus()
                }
                else if (email.isEmpty()) {
                    binding.enterEmail.error = "Please enter the email address."
                    binding.enterEmail.requestFocus()
                }
                else if (password.isEmpty()) {
                    binding.enterPasswordLayout.endIconMode = TextInputLayout.END_ICON_NONE
                    binding.enterPassword.error = "Please enter the password."
                    binding.enterPassword.requestFocus()

                    //enable back when the text field is filled
                    binding.enterPassword.doAfterTextChanged {
                        binding.enterPasswordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    }
                }
            }
        }
    }

    //register -> login 
    private fun handleBackPressed() {
        findNavController().navigateUp()
    }
    private fun setClickListeners() {
        binding.upBtn.setOnClickListener {
            handleBackPressed()
        }

        binding.loginBtn.setOnClickListener {
            handleBackPressed()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            handleBackPressed()
        }
    }

    private fun createUserWithEmailAndPassword(email: String, password: String) {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Creating...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    createUser(task.result.user)
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    findNavController().navigateUp()
                    showToast("Account created successfully!")
                } else {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    // show the error message
                    showToast(task.exception?.message.toString())
                }
            }
    }

    private fun createUser(account: FirebaseUser?) {
        val url = getString(R.string.url_server) + getString(R.string.url_create_user) +
                "?userId=" + account?.uid +
                "&userName=" + binding.enterUsername.text +
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
