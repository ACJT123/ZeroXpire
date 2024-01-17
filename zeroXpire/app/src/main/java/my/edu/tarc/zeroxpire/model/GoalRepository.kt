package my.edu.tarc.zeroxpire.model

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import my.edu.tarc.zeroxpire.model.GoalDao
import my.edu.tarc.zeroxpire.model.Goal

class GoalRepository(private val goalDao: GoalDao){
    //Room execute all queries on a separate thread
    val allGoals: LiveData<List<Goal>> = goalDao.getAllGoals()


    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun add(goal: Goal){
        goalDao.addGoal(goal)
    }

    @WorkerThread
    suspend fun deleteAll(){
        goalDao.deleteAllGoal()
    }

    @WorkerThread
    suspend fun delete(goal: Goal){
        goalDao.deleteGoal(goal)
    }

    @WorkerThread
    suspend fun update(goal: Goal){
        goalDao.updateGoal(goal)
    }

    @WorkerThread
    fun getLatestGoal(): Goal?{
        return goalDao.getLatestGoal()
    }

    @WorkerThread
    fun getGoalByGoalId(goalId: Int): LiveData<List<Goal>>{
        return goalDao.getGoalByGoalId(goalId)
    }

//    @WorkerThread
//    suspend fun updateName(id: Int, newName: String) {
//        goalDao.updateName(id, newName)
//    }
//
//    @WorkerThread
//    suspend fun getGoalById(id: Int){
//        goalDao.getGoalById(id)
//    }

//    @WorkerThread
//    suspend fun sortByName(goal: Goal){
//        goalDao.sortByName(goal)
//    }
}