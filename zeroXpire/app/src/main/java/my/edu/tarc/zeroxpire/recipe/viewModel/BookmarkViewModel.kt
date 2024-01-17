package my.edu.tarc.zeroxpire.recipe.viewModel

import android.util.Log
import android.view.View
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.recipe.Recipe
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException

class BookmarkViewModel {
    fun getBookmarksByUserID(userId: String,
                     view: View,
                     callback: (ArrayList<Recipe>) -> Unit) {
        val url = StringBuilder(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.bookmarkGetBookmarksByUserID))
            .append("?userId=")
            .append(userId)
            .toString()
        Log.d("Get bookmarks", "URL: $url")

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
                        Log.d("get bookmark JSON", jsonRecipe.toString())
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
                Log.d("Get bookmarks", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("Get bookmarks", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("Get bookmarks", "Error Response: ${error.message}")
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

    fun removeFromBookmarks(
        userId: String,
        recipeID: String,
        view: View,
        callback: (Boolean) -> Unit
    ) {
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.bookmarkRemoveFromBookmarks))
            .append("?userId=")
            .append(userId)
            .append("&recipeID=")
            .append(recipeID)
            .toString()
        Log.d("bookmark delete", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("bookmark delete", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("bookmark delete", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("bookmark delete", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("bookmark delete", "Error Response: ${error.message}")
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

    fun removeArrayFromBookmarks(
        userId: String,
        recipeIDArrayList: ArrayList<String>,
        view: View,
        callback: (Boolean) -> Unit
    ) {
        val recipeIDArray = StringBuilder()
        recipeIDArrayList.joinTo(recipeIDArray, "&recipeIDArr[]=", "&recipeIDArr[]=")
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.bookmarkRemoveArrayFromBookmarks))
            .append("?userId=")
            .append(userId)
            .append(recipeIDArray.toString())
            .toString()
        Log.d("bookmark delete array", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("bookmark delete array", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("bookmark delete array", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("bookmark delete array", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("bookmark delete array", "Error Response: ${error.message}")
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


    fun addToBookmark(
        userId: String,
        recipeID: String,
        view: View,
        callback: (Boolean) -> Unit
    ) {
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.bookmarkAddToBookmark))
            .append("?userId=")
            .append(userId)
            .append("&recipeID=")
            .append(recipeID)
            .toString()
        Log.d("bookmark create", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("bookmark create", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("bookmark create", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("bookmark create", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("bookmark create", "Error Response: ${error.message}")
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

    fun addToBookmarksFromArray(
        userId: String,
        recipeIDArrayList: ArrayList<String>,
        view: View,
        callback: (Boolean) -> Unit
    ) {
        val recipeIDArray = StringBuilder()
        recipeIDArrayList.joinTo(recipeIDArray, "&recipeIDArr[]=", "&recipeIDArr[]=")
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.bookmarkAddToBookmarksFromArray))
            .append("?userId=")
            .append(userId)
            .append(recipeIDArray)
            .toString()
        Log.d("bookmark create array", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("bookmark create array", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("bookmark create array", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("bookmark create array", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("bookmark create array", "Error Response: ${error.message}")
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

}