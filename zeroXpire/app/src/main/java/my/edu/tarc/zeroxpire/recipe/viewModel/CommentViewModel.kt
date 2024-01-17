package my.edu.tarc.zeroxpire.recipe.viewModel

import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.recipe.Comment
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date

class CommentViewModel {
    fun createComment(comment: Comment,
                      view: View,
                      callback: (Boolean) -> Unit)
    {
        val url = StringBuilder(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.commentCreateURL))
            .append("?recipeID=")
            .append(comment.recipeID)
            .append("&userId=")
            .append(comment.userID)
            .append("&comment=")
            .append(comment.comment)
            .append("&replyTo=")
            .append(comment.replyTo)
            .toString()
        Log.d("Create Comment", "URL: $url")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("Create Comment", response.toString())
                    if (response != null) {
                        //get response
                        callback(response.getBoolean("success"))
                    }
                    callback(true)
                } catch (e: UnknownHostException) {
                    Log.d("Create Comment", "Unknown Host: ${e.message}")
                }
                catch (e: Exception) {
                    Log.d("Create Comment", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("Create Comment", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }


    fun getComments(recipeID: Int,
                      view: View,
                      callback: (ArrayList<Comment>) -> Unit)
    {
        val commentArrayList = ArrayList<Comment>()
        val url = StringBuilder(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.commentGetCommentsByRecipeIDURL))
            .append("?recipeID=")
            .append(recipeID)
            .toString()
        Log.d("Get Comment", "URL: $url")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        //get response
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()


                        for (i in 0 until size) {
                            val jsonComment: JSONObject = jsonArray.getJSONObject(i)
                            Log.d("JSON", jsonComment.toString())
                            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

                            val comment = Comment()
                            comment.recipeID = jsonComment.getInt("recipeID")
                            comment.userID = jsonComment.getString("userId")
                            comment.dateTime = sdf.parse(jsonComment.getString("dateTime")) as Date
                            comment.comment = jsonComment.getString("comment")
                            comment.likesCount = jsonComment.getInt("likes")
                            comment.replyTo = jsonComment.getString("replyTo")
                            comment.username = jsonComment.getString("userName")
                            commentArrayList.add(comment)
                        }
                    }
                    Log.d("Get Comment", "ArrayList: $commentArrayList")
                    callback(commentArrayList)
                } catch (e: UnknownHostException) {
                    Log.d("Get Comment", "Unknown Host: ${e.message}")
                }
                catch (e: Exception) {
                    Log.d("Get Comment", "Response: ${e.message}")
                }
            },
            { error ->
                Log.d("Get Comment", "Error Response: ${error.message}")
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )
        WebDB.getInstance(view.context).addToRequestQueue(jsonObjectRequest)
    }


}