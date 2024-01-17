package my.edu.tarc.zeroxpire.recipe

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.util.*

data class Comment(
    var recipeID: Int = -1,
    var userID: String = "",
    var dateTime: Date = Date(0),
    var comment: String = "",
    var likesCount: Int = 0,
    var replyTo: String = "",
    var username: String = ""
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSqlDate(): LocalDateTime? {
        return LocalDateTime.parse(dateTime.toString())
    }
}