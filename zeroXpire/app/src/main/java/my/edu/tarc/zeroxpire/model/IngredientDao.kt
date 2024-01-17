package my.edu.tarc.zeroxpire.model

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface IngredientDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIngredient(ingredient: Ingredient)

    @Query("SELECT * FROM Ingredient")
    fun getAllIngredients(): LiveData<List<Ingredient>>

    @Query("SELECT * FROM Ingredient WHERE ingredientGoalId IS NULL")
    fun getAllIngredientsWithoutGoalId(): LiveData<List<Ingredient>>
    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("DELETE FROM Ingredient WHERE ingredientId = :ingredientId")
    suspend fun deleteIngredientUsingId(ingredientId: Int)

    @Query("DELETE FROM Ingredient")
    suspend fun deleteAllIngredient()

    @Query("UPDATE Ingredient SET ingredientName = :name WHERE ingredientId = :id")
    suspend fun updateName(id: Int?, name: String?)

    @Query("UPDATE Ingredient SET expiryDate = :date WHERE ingredientId = :id")
    suspend fun updateExpiryDate(id: Int?, date: Long)

//    @Query("SELECT * FROM Ingredient WHERE ingredientId = :id")
//    suspend fun getIngredientById(id: Int): Ingredient?

    @Query("SELECT * FROM Ingredient ORDER BY ingredientName ASC")
    fun sortByName(): LiveData<List<Ingredient>>

    //default sorting method
    @Query("SELECT * FROM Ingredient ORDER BY dateAdded ASC")
    fun sortByAdded(): LiveData<List<Ingredient>>

    @Query("SELECT * FROM Ingredient ORDER BY expiryDate ASC")
    fun sortByExpiryDate(): LiveData<List<Ingredient>>

    @Query("SELECT * FROM Ingredient ORDER BY expiryDate DESC")
    fun sortByExpiryDateDesc(): LiveData<List<Ingredient>>


    @Query("SELECT * FROM Ingredient WHERE expiryDate >= :startDate AND expiryDate <= :endDate")
    fun getIngredientsWithinPeriod(startDate: Long, endDate: Long): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE ingredientId = :ingredientId")
    fun getIngredientsById(ingredientId: Int): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE ingredientGoalId = :ingredientGoalId")
    fun getIngredientsByGoalId(ingredientGoalId: Int): LiveData<List<Ingredient>>

    @Query("UPDATE Ingredient SET ingredientGoalId = :goalId WHERE ingredientId = :ingredientId")
    suspend fun updateGoalId(goalId: Int?, ingredientId: Int)

}