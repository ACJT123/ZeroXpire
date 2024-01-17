package my.edu.tarc.zeroxpire.model

import androidx.room.*
import my.edu.tarc.zeroxpire.DateConverter
import java.util.*

@Entity
@TypeConverters(DateConverter::class)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val goalId: Int,
    val goalName: String,
    val targetCompletionDate: Date,
    val dateCreated: Date,
    val completedDate: Date?,
    val uncompletedDate: Date?,
    val userId: String
)
