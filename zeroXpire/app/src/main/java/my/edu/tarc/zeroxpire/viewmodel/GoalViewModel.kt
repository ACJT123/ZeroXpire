package my.edu.tarc.zeroxpire.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.edu.tarc.zeroxpire.model.GoalRepository
import my.edu.tarc.zeroxpire.model.GoalDatabase
import my.edu.tarc.zeroxpire.model.Goal
import my.edu.tarc.zeroxpire.model.Ingredient

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalRepository
    val goalList: LiveData<List<Goal>>
    private val sortedGoalList: MutableLiveData<List<Goal>> = MutableLiveData()

    init {
        val goalDao = GoalDatabase.getDatabase(application).goalDao()
        repository = GoalRepository(goalDao)
        goalList = repository.allGoals
        sortedGoalList.value = goalList.value
    }

    fun addGoal(goal: Goal) = viewModelScope.launch {
        repository.add(goal)
    }

    fun deleteAllGoals() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun deleteGoal(goal: Goal) = viewModelScope.launch {
        repository.delete(goal)
    }

    fun updateGoal(goal: Goal) = viewModelScope.launch {
        repository.update(goal)
    }

    fun getLatestGoal(): Goal? {
        return repository.getLatestGoal()
    }

    fun getGoalByGoalId(goalId: Int): LiveData<List<Goal>> {
        return repository.getGoalByGoalId(goalId)
    }


//    fun updateGoalName(id: Int, newName: String) = viewModelScope.launch {
//        repository.updateName(id, newName)
//    }

//    fun getGoalById(id: Int) = viewModelScope.launch {
//        repository.getGoalById(id)
//    }
//
//    fun filterGoals(query: String) {
//        val filteredList = goalList.value?.filter { goal ->
//            goal.goalName.contains(query, ignoreCase = true)
//        } ?: emptyList()
//        sortedGoalList.value = filteredList
//    }

}
