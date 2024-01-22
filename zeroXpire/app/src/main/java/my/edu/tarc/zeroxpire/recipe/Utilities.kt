package my.edu.tarc.zeroxpire.recipe

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.zeroxpire.R
import java.util.*
import kotlin.collections.ArrayList

class Utilities {

    fun createNewLinearLayout(
        view: View,
        left: Int = 0,
        right: Int = 0,
        top: Int = 0,
        bottom: Int = 0
    ): LinearLayout {
        val newLinearLayout = LinearLayout(view.context, null)

        //apply attributes
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(left,top,right,bottom)

        newLinearLayout.layoutParams = layoutParams
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        return newLinearLayout
    }

    fun createNewCheckBox(
        view: View,
        text: String = "",
        typeface: Int = Typeface.NORMAL
    ): CheckBox {
        val newCheckBox = CheckBox(view.context, null, R.style.CheckBox)

        //apply attributes
        newCheckBox.text = text
        newCheckBox.textSize = 16F
        newCheckBox.setTypeface(null, typeface)
        newCheckBox.buttonDrawable = AppCompatResources.getDrawable(view.context, R.drawable.baseline_check_box_outline_blank_24)
        newCheckBox.gravity = Gravity.CENTER_VERTICAL
        newCheckBox.isVisible = true
        newCheckBox.buttonTintList = ColorStateList.valueOf(
            ContextCompat.getColor(view.context, R.color.btnColor))

        return newCheckBox
    }

    fun createNewEditText(view: View, text: String = "", hint: String = ""): EditText {
        val newEditText = EditText(view.context)
        //apply attributes
        newEditText.background = null
        newEditText.gravity = Gravity.START or Gravity.TOP
        newEditText.hint = hint
        newEditText.isSingleLine = false
        newEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        newEditText.textSize = 16F
        newEditText.setText(text)

        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0,0,0,0)
        newEditText.setPadding(0)
        newEditText.layoutParams = layoutParams

        return newEditText
    }

    fun createNewTextView(
        view: View,
        text: String = "",
        typeface: Int = Typeface.NORMAL,
        textSize: Float = 16F): TextView {
        val newTextView = TextView(view.context)

        //apply attributes
        newTextView.text = text
        newTextView.textSize = textSize
        newTextView.setTypeface(null, typeface)
        newTextView.isVisible = true

        return newTextView
    }

    fun createNewChat(view: View, text: String, by: String, gravity: Int): LinearLayout {
        val layoutParamsLinearLayout = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        if (gravity == Gravity.START) layoutParamsLinearLayout.setMargins(12, 12,48,12)
        else layoutParamsLinearLayout.setMargins(48,12,12,12)

        val layoutParamsCardView = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsCardView.setMargins(5,10,5,10)

        val chatLinearLayout = LinearLayout(view.context)
        chatLinearLayout.gravity = gravity
        chatLinearLayout.layoutParams = layoutParamsLinearLayout

        val cardView = CardView(view.context)
        cardView.radius = 60f
        cardView.cardElevation = 12f
        cardView.setContentPadding(40,25,40,25)
        cardView.layoutParams = layoutParamsCardView

        val chatContentLinearLayout = LinearLayout(view.context)
        chatContentLinearLayout.orientation = LinearLayout.VERTICAL

        val user = TextView(view.context)
        user.text = by
        user.setTypeface(null, Typeface.BOLD)
        user.textSize = 16f
        user.setPadding(0,8,8,12)

        val chat = TextView(view.context)
        chat.text = text

        chatContentLinearLayout.addView(user)
        chatContentLinearLayout.addView(chat)
        cardView.addView(chatContentLinearLayout)
        chatLinearLayout.addView(cardView)

        return chatLinearLayout
    }

    fun <T, VH : RecyclerView.ViewHolder> createDragHelper(
        list: ArrayList<T>,
        adapter: RecyclerView.Adapter<VH>
    ) : ItemTouchHelper{
        val dragHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                viewHolder.itemView.elevation = 16F

                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                Collections.swap(list, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder?.itemView?.elevation = 0F
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        })
        return dragHelper
    }

    fun createDeleteAlert(context: Context): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_title)
        builder.setCancelable(false)

        return builder
    }
}