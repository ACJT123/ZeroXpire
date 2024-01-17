package my.edu.tarc.zeroxpire.model

import androidx.room.Embedded
import androidx.room.Relation

data class GoalWithIngredients(
    @Embedded val goal: Goal,
    @Relation(
        parentColumn = "goalId",
        entityColumn = "ingredientGoalId"
    )
    val ingredient: List<Ingredient>
)
