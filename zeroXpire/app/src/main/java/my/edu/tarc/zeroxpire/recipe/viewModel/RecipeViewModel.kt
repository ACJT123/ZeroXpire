package my.edu.tarc.zeroxpire.recipe.viewModel

import android.content.Context
import android.util.Log
import android.view.View
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.recipe.Recipe
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException


class
RecipeViewModel {

    fun getRecommend(url: String,
                     view: View,
                     callback: (ArrayList<Recipe>) -> Unit) {
        val recipeArrayList = ArrayList<Recipe>()
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    val strResponse = response.toString()
                    val jsonResponse = JSONObject(strResponse)
                    val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                    val size: Int = jsonArray.length()

                    for (i in 0 until size) {
                        val jsonRecipe: JSONObject = jsonArray.getJSONObject(i)
                        Log.d("JSON", jsonRecipe.toString())
                        val id = jsonRecipe.getInt("recipeID")
                        val title = jsonRecipe.getString("title")
                        val instructionsLink = jsonRecipe.getString("instructionsLink")
                        val imageLink = jsonRecipe.getString("imageLink")
                        val note = jsonRecipe.getString("note")
                        val authorID = jsonRecipe.getString("author")
                        val authorName = jsonRecipe.getString("userName")
                        val ingredientNames = jsonRecipe.getString("ingredientName")
                        recipeArrayList.add(
                            Recipe(
                                id,
                                title,
                                instructionsLink,
                                imageLink,
                                note,
                                authorID,
                                authorName,
                                ingredientNames = ingredientNames
                            )
                        )
                    }
                    callback(recipeArrayList)
                }
            } catch (e: UnknownHostException) {
                Log.d("RecipeAdapter", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("RecipeAdapter", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("FK", "Error Response: ${error.message}")
            callback(recipeArrayList)
        }


        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }


    fun searchInDatabase(
        keyword: String,
        selectedIngredients: MutableList<Ingredient>,
        context: Context,
        callback: (ArrayList<Recipe>) -> Unit
    ){
        val recipeArrayList = ArrayList<Recipe>()
        val ingredients = StringBuilder()

        if (selectedIngredients.isEmpty()) {
            ingredients.append("&ingredientNameArr[]=")
        }else {
            selectedIngredients.forEach {
                ingredients.append("&ingredientNameArr[]=${it.ingredientName}")
            }
        }
        val url = StringBuilder()
            .append(context.getString(R.string.url_server))
            .append(context.getString(R.string.recipeSearchURL))
            .append("?keyword=$keyword")
            .append(ingredients)
            .toString()

        Log.d("search url", url)

        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    val strResponse = response.toString()
                    val jsonResponse = JSONObject(strResponse)
                    val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                    val size: Int = jsonArray.length()

                    for (i in 0 until size) {
                        val jsonRecipe: JSONObject = jsonArray.getJSONObject(i)
                        Log.d("JSON", jsonRecipe.toString())
                        val id = jsonRecipe.getInt("recipeID")
                        val title = jsonRecipe.getString("title")
                        val instructionsLink = jsonRecipe.getString("instructionsLink")
                        val imageLink = jsonRecipe.getString("imageLink")
                        val note = jsonRecipe.getString("note")
                        val authorID = jsonRecipe.getString("author")
                        val authorName = jsonRecipe.getString("userName")
                        val ingredientNames = jsonRecipe.getString("ingredientName")
                        recipeArrayList.add(
                            Recipe(
                                id,
                                title,
                                instructionsLink,
                                imageLink,
                                note,
                                authorID,
                                authorName,
                                ingredientNames = ingredientNames
                            )
                        )
                    }
                    callback(recipeArrayList)
                }
            } catch (e: UnknownHostException) {
                Log.d("RecipeAdapter", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("RecipeAdapter", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("FK", "Error Response: ${error.message}")
            callback(recipeArrayList)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            successListener,
            errorListener
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }



}