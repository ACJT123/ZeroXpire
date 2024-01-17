package my.edu.tarc.zeroxpire.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.model.IngredientDatabase
import my.edu.tarc.zeroxpire.model.IngredientRepository
import java.util.Date

class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IngredientRepository
    val ingredientList: LiveData<List<Ingredient>>
    private val sortedIngredientList: MutableLiveData<List<Ingredient>> = MutableLiveData()

    init {
        val ingredientDao = IngredientDatabase.getDatabase(application).ingredientDao()
        repository = IngredientRepository(ingredientDao)
        ingredientList = repository.allIngredients
        sortedIngredientList.value = ingredientList.value
    }

    fun addIngredient(ingredient: Ingredient) = viewModelScope.launch {
        repository.add(ingredient)
    }

    fun deleteAllIngredients() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun deleteIngredient(ingredient: Ingredient) = viewModelScope.launch {
        repository.delete(ingredient)
    }

    fun deleteIngredientUsingId(ingredientId: Int) = viewModelScope.launch {
        repository.deleteIngredient(ingredientId)
    }

    fun updateIngredient(ingredient: Ingredient) = viewModelScope.launch {
        repository.update(ingredient)
    }

    fun updateIngredientName(id: Int, newName: String) = viewModelScope.launch {
        repository.updateName(id, newName)
    }

    fun updateExpiryDate(id: Int, newDate: Long) = viewModelScope.launch {
        repository.updateExpiryDate(id, newDate)
    }

    fun getAllIngredientsWithoutGoalId(): LiveData<List<Ingredient>>{
        return repository.getAllIngredientsWithoutGoalId()
    }

    fun getIngredientsByGoalId(ingredientGoalId: Int): LiveData<List<Ingredient>>{
        return repository.getIngredientsByGoalId(ingredientGoalId)
    }

    fun sortByName(): LiveData<List<Ingredient>> {
        return repository.sortByName()
    }

    fun sortByExpiryDate(): LiveData<List<Ingredient>> {
        return repository.sortByExpiryDate()
    }

    fun sortByExpiryDateDesc(): LiveData<List<Ingredient>> {
        return repository.sortByExpiryDateDesc()
    }

    fun sortByDateAdded(): LiveData<List<Ingredient>> {
        return repository.sortByDateAdded()
    }

    fun getIngredientById(id: Int): LiveData<List<Ingredient>> {
        return repository.getIngredientById(id)
    }

    fun updateGoalId(goalId: Int, ingredientId: Int) = viewModelScope.launch {
        repository.updateGoalId(goalId, ingredientId)
    }
//
//    fun filterIngredients(query: String) {
//        val filteredList = ingredientList.value?.filter { ingredient ->
//            ingredient.ingredientName.contains(query, ignoreCase = true)
//        } ?: emptyList()
//        sortedIngredientList.value = filteredList
//    }

//    fun getIngredientsWithinPeriod(startDate: Long, endDate: Long): LiveData<List<Ingredient>> {
//        return repository.getIngredientsWithinPeriod(startDate, endDate)
//    }


}
