package my.edu.tarc.zeroxpire.view.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentEnterVeriCodeBinding

class EnterVeriCodeFragment : Fragment() {
    private lateinit var binding: FragmentEnterVeriCodeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEnterVeriCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.upBtn.setOnClickListener {
            findNavController().navigate(R.id.action_enterVeriCodeFragment_to_forgotPasswordFragment)
        }

        binding.resendCodeBtn.setOnClickListener {
            //TODO: resend code function
        }

        binding.confirmBtn.setOnClickListener {
            findNavController().navigate(R.id.action_enterVeriCodeFragment_to_resetPasswordFragment)
        }

        val email: String = "hi"
        binding.sentToEmailDescription.text = "Enter the verification code we sent to $email."
    }
}