package my.edu.tarc.zeroxpire.model

import android.support.annotation.WorkerThread
import androidx.lifecycle.LiveData
import java.util.Date

class IngredientRepository(private val ingredientDao: IngredientDao){
    //Room execute all queries on a separate thread
    val allIngredients: LiveData<List<Ingredient>> = ingredientDao.getAllIngredients()


    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun add(ingredient: Ingredient){
        ingredientDao.addIngredient(ingredient)
    }

    @WorkerThread
    suspend fun deleteAll(){
        ingredientDao.deleteAllIngredient()
    }

    @WorkerThread
    suspend fun delete(ingredient: Ingredient){
        ingredientDao.deleteIngredient(ingredient)
    }

    @WorkerThread
    suspend fun update(ingredient: Ingredient){
        ingredientDao.updateIngredient(ingredient)
    }

    @WorkerThread
    suspend fun deleteIngredient(ingredientId: Int){
        ingredientDao.deleteIngredientUsingId(ingredientId)
    }

    @WorkerThread
    fun getIngredientsWithinPeriod(startDate: Long, endDate: Long): LiveData<List<Ingredient>> {
        return ingredientDao.getIngredientsWithinPeriod(startDate, endDate)
    }

    @WorkerThread
    suspend fun updateName(id: Int, newName: String) {
        ingredientDao.updateName(id, newName)
    }

    @WorkerThread
    suspend fun updateExpiryDate(id: Int, newDate: Long) {
        ingredientDao.updateExpiryDate(id, newDate)
    }

    @WorkerThread
    fun getIngredientById(id: Int): LiveData<List<Ingredient>>{
        return ingredientDao.getIngredientsById(id)
    }

    @WorkerThread
    fun getAllIngredientsWithoutGoalId(): LiveData<List<Ingredient>>{
        return ingredientDao.getAllIngredientsWithoutGoalId()
    }

    @WorkerThread
    fun sortByName(): LiveData<List<Ingredient>> {
        return ingredientDao.sortByName()
    }

    @WorkerThread
    fun sortByExpiryDate(): LiveData<List<Ingredient>> {
        return ingredientDao.sortByExpiryDate()
    }


    @WorkerThread
    fun sortByExpiryDateDesc(): LiveData<List<Ingredient>> {
        return ingredientDao.sortByExpiryDateDesc()
    }


    @WorkerThread
    fun sortByDateAdded(): LiveData<List<Ingredient>> {
        return ingredientDao.sortByAdded()
    }

    @WorkerThread
    fun getIngredientsByGoalId(ingredientGoalId: Int): LiveData<List<Ingredient>>{
        return ingredientDao.getIngredientsByGoalId(ingredientGoalId)
    }

    @WorkerThread
    suspend fun updateGoalId(goalId: Int, ingredientId: Int){
        ingredientDao.updateGoalId(goalId, ingredientId)
    }


}