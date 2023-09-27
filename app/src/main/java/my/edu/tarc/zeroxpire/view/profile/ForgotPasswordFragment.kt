package my.edu.tarc.zeroxpire.view.profile

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.zeroxpire.MainActivity
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {
    private lateinit var binding: FragmentForgotPasswordBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()


        // Inflate the layout for this fragment
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.upBtn.setOnClickListener {
            findNavController().popBackStack()
        }

// Inside the binding.confirmBtn.setOnClickListener block
        binding.confirmBtn.setOnClickListener {
            val email = binding.enterEmail.text.toString()

            if (email.isNotEmpty()) {
                // Check if the email already exists in Firebase Authentication
                auth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener { signInMethodsTask ->
                        if (signInMethodsTask.isSuccessful) {
                            val signInMethods = signInMethodsTask.result?.signInMethods
                            if (signInMethods.isNullOrEmpty()) {
                                // Email doesn't exist, you can show an error or proceed as needed
                                binding.enterEmail.error = "This email is not registered."
                                binding.enterEmail.requestFocus()
                            } else {
                                // Email exists, proceed with sending the password reset email
                                val progressDialog = ProgressDialog(requireContext())
                                progressDialog.setMessage("Sending password resetting email to your mailbox...")
                                progressDialog.setCancelable(false)
                                progressDialog.show()
                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { resetEmailTask ->
                                        progressDialog.dismiss()
                                        if (resetEmailTask.isSuccessful) {
                                            dialog()
                                        } else {
                                            toast(resetEmailTask.exception?.message ?: "Password reset email could not be sent.")
                                        }
                                    }
                            }
                        } else {
                            toast("Error checking email existence: ${signInMethodsTask.exception?.message}")
                        }
                    }
            } else {
                binding.enterEmail.error = "Please enter the email."
                binding.enterEmail.requestFocus()
            }
        }

    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun dialog(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("We have sent an email for password resetting to your mailbox").setCancelable(false)
            .setPositiveButton("Got it") { dialog, id ->
                findNavController().navigate(R.id.loginFragment)
                findNavController().clearBackStack(R.id.loginFragment)
            }
        val alert = builder.create()
        alert.show()
    }

}