package my.edu.tarc.zeroxpire.view.others

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentTermsAndConditionsBinding



class TermsAndConditionsFragment : Fragment() {
    private lateinit var binding: FragmentTermsAndConditionsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTermsAndConditionsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.upBtn.setOnClickListener {
            navigation()
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
                navigation()
                val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
                view.visibility = View.VISIBLE

                val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                add.visibility = View.VISIBLE
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun navigation(){
        findNavController().navigateUp()
    }
}