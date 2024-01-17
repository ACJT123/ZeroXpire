package my.edu.tarc.zeroxpire.goal

import my.edu.tarc.zeroxpire.model.Goal

interface GoalClickListener {
    fun onGoalClick(goal: Goal)
}