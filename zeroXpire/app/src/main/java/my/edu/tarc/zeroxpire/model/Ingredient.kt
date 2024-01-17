package my.edu.tarc.zeroxpire.model

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import my.edu.tarc.zeroxpire.BitmapTypeConverter
import my.edu.tarc.zeroxpire.DateConverter
import java.util.Date


//@Entity(
//    foreignKeys = [
//        ForeignKey(
//            entity = Goal::class,
//            childColumns = ["ingredientGoalId"],
//            parentColumns = ["goalId"]
//        )
//    ]
//)
@Entity
@TypeConverters(DateConverter::class, BitmapTypeConverter::class)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val ingredientId: Int,
    val ingredientName: String,
    val expiryDate: Date,
    val dateAdded: Date?,
    val ingredientImage: String?,
    val ingredientCategory: String,
    val isDelete: Int,
    val ingredientGoalId: Int?,
    val userId: String,
)
