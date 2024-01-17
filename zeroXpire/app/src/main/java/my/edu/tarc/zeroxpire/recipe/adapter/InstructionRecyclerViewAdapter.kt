package my.edu.tarc.zeroxpire.recipe.adapter

import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.fragment.RecipeCreateFragment
import my.edu.tarc.zeroxpire.recipe.viewHolder.InstructionRecyclerViewHolder

class InstructionRecyclerViewAdapter(
    private val context: RecipeCreateFragment,
    val instructionArrayList: ArrayList<String>
    ) : RecyclerView.Adapter<InstructionRecyclerViewHolder>()  {

    private lateinit var parentContext: Context

    private var focusingOn: InstructionRecyclerViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionRecyclerViewHolder {
        parentContext = parent.context

        val view = LayoutInflater.from(parentContext).inflate(R.layout.instruction_frame, parent, false)
        return InstructionRecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return instructionArrayList.size
    }

    override fun onBindViewHolder(holder: InstructionRecyclerViewHolder, position: Int) {
        // declaration: views
        val currentView = holder.getView()

        val instructionTextInputLayout = currentView.findViewById<TextInputLayout>(R.id.instructionTextInputLayout)
        val instructionTextInputEditText = currentView.findViewById<TextInputEditText>(R.id.instructionTextInputEditText)
        val addInstructionImageView = currentView.findViewById<ImageView>(R.id.addInstructionImageView)
        val dragInstructionImageView = currentView.findViewById<ImageView>(R.id.dragInstructionImageView)

        instructionTextInputLayout.hint = parentContext.getString(R.string.step_with_number, position)

        instructionTextInputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        instructionTextInputEditText.imeOptions = EditorInfo.IME_ACTION_NEXT

        instructionTextInputEditText.setText(instructionArrayList[position])

        instructionTextInputEditText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                instructionArrayList[position] = (v as TextInputEditText).text.toString()
                focusingOn = null
            }else {
                focusingOn = holder
            }
        }

        dragInstructionImageView.setOnTouchListener { _, _ ->
            context.startDragging(holder)
            return@setOnTouchListener true
        }

        addInstructionImageView.setOnClickListener {
            instructionArrayList[position] = instructionTextInputEditText.text.toString()
            addInstruction(position, focusingOn = focusingOn)
        }

    }

    fun addInstruction(pos: Int, text: String = "", focusingOn: InstructionRecyclerViewHolder? = null) {
        //save instruction that user is typing
        if (focusingOn != null) {
            val focusedText = focusingOn.getView().findViewById<TextInputEditText>(R.id.instructionTextInputEditText).text.toString()
            instructionArrayList[focusingOn.bindingAdapterPosition] = focusedText
        }

        //add 1 to add at next line
        var addPos = pos + 1
        //add new instruction
        if (addPos > instructionArrayList.size){
            addPos = instructionArrayList.size
        }
        instructionArrayList.add(addPos, text)
        notifyItemInserted(addPos)
        Log.d("instructionArrayList", "$instructionArrayList")
        val size = instructionArrayList.size - addPos
        notifyItemRangeChanged(addPos, size+1)
    }

}