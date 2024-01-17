package my.edu.tarc.zeroxpire.ingredient

import my.edu.tarc.zeroxpire.model.Ingredient

interface IngredientClickListener {
    fun onIngredientClick(ingredient: Ingredient)
}