package my.edu.tarc.zeroxpire.model

import android.content.Context
import android.media.Image
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import my.edu.tarc.zeroxpire.BitmapTypeConverter
import my.edu.tarc.zeroxpire.DateConverter
import my.edu.tarc.zeroxpire.view.ingredient.ImageConverters

@Database(entities = [Ingredient::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class, BitmapTypeConverter::class)
abstract class IngredientDatabase : RoomDatabase() {

    abstract fun ingredientDao(): IngredientDao

    companion object{

        @Volatile
        private var INSTANCE: IngredientDatabase? = null

        fun getDatabase(context: Context): IngredientDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IngredientDatabase::class.java,
                    "ingredient_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}