package my.edu.tarc.zeroxpire.view.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding

    private val CAMERA_REQUEST = 100
    private val STORAGE_REQUEST = 200
    private val IMAGEPICK_GALLERY_REQUEST = 300
    private val IMAGE_PICKCAMERA_REQUEST = 400

    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()



        setFragmentResultListener("requestName") { key, bundle ->
            val result = bundle.getString("name")
            binding.editUsername.setText(result)
        }

        val user = auth.currentUser
        val isGoogleSignIn = user?.providerData?.any { provider ->
            provider.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false

        if (isGoogleSignIn) {
            // User has logged in with Google account
            // Handle the appropriate logic here
            binding.editPasswordLayout.visibility = View.GONE
            setFragmentResultListener("requestProfilePic") { key, bundle ->
                val result = bundle.getString("profilePic")
                Glide.with(this)
                    .load(result)
                    .into(binding.userProfilePicture)
            }
        } else {
            // User has not logged in with Google account
            // Handle the appropriate logic here
            binding.editPasswordLayout.visibility = View.VISIBLE
            Glide.with(this)
                .load(R.drawable.baseline_person_24)
                .into(binding.userProfilePicture)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.upBtn.setOnClickListener {
            findNavController().navigateUp()
            val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
            view.visibility = View.VISIBLE

            val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            add.visibility = View.VISIBLE
        }


        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button press here
                // You can perform any necessary actions or navigation

                // For example, navigate to a different fragment
                findNavController().navigateUp()
                val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
                view.visibility = View.VISIBLE

                val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                add.visibility = View.VISIBLE
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        binding.userProfilePicture.setOnClickListener {
            pickFromGallery()
        }


        //only can display the usernames those are registered under Google
//        auth.currentUser?.displayName?.let {
//            toast(it)
//        }

    }

    private fun pickFromGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(intent, IMAGEPICK_GALLERY_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGEPICK_GALLERY_REQUEST && resultCode == RESULT_OK) {
            val imageUri = data?.data
            Glide.with(this)
                .load(imageUri)
                .into(binding.userProfilePicture)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}