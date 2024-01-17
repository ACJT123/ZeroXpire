package my.edu.tarc.zeroxpire.recipe

data class Recipe(var recipeID: Int = -1,
                  var title: String = "",
                  var instructionsLink: String = "",
                  var imageLink: String = "",
                  var note: String = "",
                  var authorID: String = "",
                  var authorName: String = "",
                  var isBookmarked: Boolean = false,
                  var ingredientNames: String = "",
                  var ingredientNamesArrayList: ArrayList<String> = ArrayList(),
                  var ingredientIDArrayList: ArrayList<String> = ArrayList())