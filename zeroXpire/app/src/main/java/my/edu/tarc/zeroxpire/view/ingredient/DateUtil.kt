package my.edu.tarc.zeroxpire.view.ingredient

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtil {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDate(dateString: String): String {
        val date = LocalDate.parse(dateString, formatter)
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDaysDifference(startDate: String, endDate: String): Long {
        val startDateObj = LocalDate.parse(startDate, formatter)
        val endDateObj = LocalDate.parse(endDate, formatter)
        return ChronoUnit.DAYS.between(startDateObj, endDateObj)
    }
}

