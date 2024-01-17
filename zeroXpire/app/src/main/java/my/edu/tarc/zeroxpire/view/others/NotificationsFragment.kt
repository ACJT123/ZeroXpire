package my.edu.tarc.zeroxpire.view.others

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import my.edu.tarc.zeroxpire.MainActivity
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding
    private var hour: Int = 0
    private var min: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val numberPicker: NumberPicker = binding.numPicker
        val textView: TextView = binding.daysReminder
        val switch: SwitchCompat = binding.switchBtn
        val timePicker: TimePicker = binding.timePicker
        val timePickerText: TextView = binding.timerPickerTextView
        val numberPickerLayout: LinearLayout = binding.numPickerLayout

        // store default switch state
        val sharedPreference = requireActivity().getSharedPreferences("sharedPreference", 0)
        //get the switch state from sharedPreference
        val switchState = sharedPreference.getBoolean("switchState", false)


        // get the dayofHour and minute from sharedPreference and display it in the timePicker
        val sharedPreference3 = requireActivity().getSharedPreferences("sharedPreference", 0)
        val hourOfDay = sharedPreference3.getInt("hourOfDay", 0)
        val minute = sharedPreference3.getInt("minute", 0)
        timePicker.hour = hourOfDay
        timePicker.minute = minute

        

        if (switchState) {
            switch.isChecked = true
            timePicker.visibility = View.VISIBLE
            timePickerText.visibility = View.VISIBLE
            numberPickerLayout.visibility = View.VISIBLE
            //set the thumb color to green
            switch.thumbTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.switchThumbColor)
            )
            //set the track color to green
            switch.trackTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.switchTrackColor)
            )
        }

            // Check the switch state and show/hide the time picker accordingly
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    timePicker.visibility = View.VISIBLE
                    timePickerText.visibility = View.VISIBLE
                    numberPickerLayout.visibility = View.VISIBLE
                    //set the thumb color to green
                    switch.thumbTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.switchThumbColor)
                    )
                    //set the track color to green
                    switch.trackTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.switchTrackColor)
                    )
                } else {
                    timePicker.visibility = View.GONE
                    timePickerText.visibility = View.GONE
                    numberPickerLayout.visibility = View.GONE
                    //reset the thumb color to default
                    switch.thumbTintList = null
                    //set the track color to green
                    switch.trackTintList = null
                }
            }

            //set the daily reminder time in sharedPreference whether is on or off
            timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
                hour = hourOfDay
                min = minute

            }
            

            //get the reminerTime from sharedPreference
            val sharedPreference2 = requireActivity().getSharedPreferences("sharedPreference", 0)
            val reminderTime = sharedPreference2.getInt("reminderTime", 3)

            numberPicker.minValue = 1
            numberPicker.maxValue = 30

            // Set initial text based on the initial value of the NumberPicker
            textView.text = "Remind you when things are expired within ${reminderTime} days"
            numberPicker.value = reminderTime

            numberPicker.setOnValueChangedListener { _, _, new ->
                textView.text = "Remind you when things are expired within $new days"
            }

            binding.upBtn.setOnClickListener {
                saveReminderDays()
                saveDailyReminderTime(timePicker.hour, timePicker.minute)
                saveIsChecked()
                findNavController().navigateUp()
                val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
                view.visibility = View.VISIBLE

                val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                add.visibility = View.VISIBLE

                //check got any changes in the switch state or not
                if (switch.isChecked != switchState) {
                    
                    //if got changes, show toast
                    Toast.makeText(
                        requireContext(),
                        "Changes applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                //check got any changes in the reminder time or not
                if (reminderTime != numberPicker.value) {
                    //if got changes, show toast
                    Toast.makeText(
                        requireContext(),
                        "Changes applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                //check got any changes in the daily reminder time or not
                if (hour != sharedPreference2.getInt("hourOfDay", 0) || min != sharedPreference2.getInt(
                        "minute",
                        0
                    )
                ) {
                    //if got changes, show toast
                    Toast.makeText(
                        requireContext(),
                        "Changes applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val mainActivity = activity as? MainActivity
                mainActivity?.loadIngredient()
                
            }

            val onBackPressedCallback = object : OnBackPressedCallback(true) {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun handleOnBackPressed() {
                    saveReminderDays()
                    saveDailyReminderTime(timePicker.hour, timePicker.minute)
                    saveIsChecked()
                    findNavController().navigateUp()
                    val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
                    view.visibility = View.VISIBLE

                    val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                    add.visibility = View.VISIBLE

                    
                //check got any changes in the reminder time or not
                if (reminderTime != numberPicker.value) {
                    //if got changes, show toast
                    Toast.makeText(
                        requireContext(),
                        "Changes applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                //check got any changes in the daily reminder time or not
                if (hour != sharedPreference2.getInt("hourOfDay", 0) || min != sharedPreference2.getInt(
                        "minute",
                        0
                    )
                ) {
                    //if got changes, show toast
                    Toast.makeText(
                        requireContext(),
                        "Changes applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                    val mainActivity = activity as? MainActivity
                    mainActivity?.loadIngredient()
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )


        }

        private fun saveReminderDays() {
            val numberPicker: NumberPicker = binding.numPicker
            val sharedPreference = requireActivity().getSharedPreferences("sharedPreference", 0)
            val editor = sharedPreference.edit()
            editor.putInt("reminderTime", numberPicker.value)
            editor.apply()
        }

        private fun saveDailyReminderTime(hour: Int, minute: Int) {
            val sharedPreference = requireActivity().getSharedPreferences("sharedPreference", 0)
            val editor = sharedPreference.edit()
            editor.putInt("hourOfDay", hour)
            editor.putInt("minute", minute)
            editor.apply()
//            Toast.makeText(
//                requireContext(),
//                "Daily reminder time set to ${hour}:$minute",
//                Toast.LENGTH_SHORT
//            ).show()
        }

        private fun saveIsChecked() {
            val switch: SwitchCompat = binding.switchBtn
            val sharedPreference = requireActivity().getSharedPreferences("sharedPreference", 0)
            val editor = sharedPreference.edit()
            editor.putBoolean("switchState", switch.isChecked)
            editor.apply()
        }
    }

