package my.edu.tarc.zeroxpire.view.ingredient

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import my.edu.tarc.zeroxpire.databinding.FragmentScannerBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {
    private lateinit var binding: FragmentScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize cameraExecutor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize textRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        binding.captureBtn.setOnClickListener {
            if (allPermissionsGranted()) {
                captureImage()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    cameraPermissions,
                    CAMERA_REQUEST_CODE
                )
            }
        }

        navigateBack()
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        // Create output file to store the captured image
        val photoFile = File(
            requireContext().externalMediaDirs.first(),
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Image saved successfully, do something with the saved image
                    val savedUri = Uri.fromFile(photoFile)
                    processImageForTextRecognition(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Error occurred while saving the image
                    Toast.makeText(
                        requireContext(),
                        "Error capturing image.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in cameraPermissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun processImageForTextRecognition(imageUri: Uri) {
        val inputImage = InputImage.fromFilePath(requireContext(), imageUri)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                processVisionText(visionText)
            }
            .addOnFailureListener { error ->
                error.printStackTrace()
            }
    }

    private fun processVisionText(visionText: Text) {
        val detectedText = visionText.text

        // Set the extracted text to the 'extractedText' TextView
        binding.extractedText.text = detectedText

        // Make the TextView visible to display the extracted text
        binding.extractedText.visibility = View.VISIBLE

        // For example, you can use the detectedText in your app as needed.
        // For now, I've set the result in a FragmentResult to be used in another fragment.
        setFragmentResult("scannedText", bundleOf("text" to detectedText))
    }

    private fun navigateBack() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textRecognizer.close()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private val cameraPermissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
