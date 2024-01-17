package my.edu.tarc.zeroxpire.view.ingredient

import androidx.annotation.ColorRes
import my.edu.tarc.zeroxpire.R

object ColorUtil {
    @ColorRes
    fun getTextColor(daysLeft: Long): Int {
        return when {
            daysLeft < 0 -> R.color.secondaryColor
            daysLeft == 0L -> R.color.textColor
            else -> R.color.btnColor
        }
    }
}