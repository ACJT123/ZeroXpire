package my.edu.tarc.zeroxpire.recipe.viewModel

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.net.toUri
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storageMetadata
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import my.edu.tarc.zeroxpire.model.Ingredient
import my.edu.tarc.zeroxpire.recipe.Recipe
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException
import kotlin.math.log

class RecipeDetailsViewModel {

    fun getRecipeById(
        userId: String,
        recipeID: String,
        view: View,
        callback: (Recipe) -> Unit
    ) {
        val recipe = Recipe()
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.recipeGetRecipeByIDURL))
            .append("?userId=")
            .append(userId)
            .append("&recipeID=")
            .append(recipeID)
            .toString()
        Log.d("Get Recipe By ID", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("JSON", response.toString())
                    val jsonRecipe = response.getJSONObject("records")
                    val ingredientNameArr = jsonRecipe.getJSONArray("ingredientName")
                    val ingredientIdArr = jsonRecipe.getJSONArray("ingredientId")

                    recipe.recipeID = jsonRecipe.getInt("recipeID")
                    recipe.title = jsonRecipe.getString("title")
                    recipe.instructionsLink = jsonRecipe.getString("instructionsLink")
                    recipe.imageLink = jsonRecipe.getString("imageLink")
                    recipe.note = jsonRecipe.getString("note")
                    recipe.authorID = jsonRecipe.getString("author")
                    recipe.authorName = jsonRecipe.getString("userName")
                    recipe.isBookmarked = jsonRecipe.getBoolean("isBookmarked")
                    for (i in 0 until ingredientNameArr.length()) {
                        recipe.ingredientNamesArrayList.add(ingredientNameArr[i].toString())
                        recipe.ingredientIDArrayList.add(ingredientIdArr[i].toString())
                    }

                    callback(recipe)
                }
            } catch (e: UnknownHostException) {
                Log.d("Get Recipe By ID", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("Get Recipe By ID", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("Get Recipe By ID", "Error Response: ${error.message}")
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


    fun editRecipe(
        ingredientsMutableList: MutableList<Ingredient>,
        instructionsArrayList: ArrayList<String>,
        recipe: Recipe,
        imageUri: Uri,
        view: View,
        callback: (Boolean) -> Unit)
    {
        val ingredientsStringBuilder = StringBuilder()
        ingredientsMutableList.forEach {
            ingredientsStringBuilder.append("&ingredientIDArr[]=${it.ingredientId}")
        }
        val firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference

        val imagePathString = "recipeImage/${recipe.title}.jpg"
        val instructionsPathString = "recipeInstructions/${recipe.title}.txt"

        val imageUploadTask = storeImageToFireBase(imagePathString, imageUri)
        val instructionsUploadTask = storeTxtToFireBase(view, instructionsArrayList, instructionsPathString)

        instructionsUploadTask.addOnSuccessListener {
            imageUploadTask.addOnSuccessListener {
                val instructionURLTask = firebaseStorageReference.child(instructionsPathString).downloadUrl
                val imageURLTask = firebaseStorageReference.child(imagePathString).downloadUrl

                val getURLTask = Tasks.whenAllSuccess<Uri>(instructionURLTask, imageURLTask)
                getURLTask.addOnSuccessListener {

                    val instructionsLink = cleanedAccessToken(it[0].toString(), recipe.title, "txt")

                    val imageLink = cleanedAccessToken(it[1].toString(), recipe.title, "jpg")

                    val ingredients = ingredientsStringBuilder.toString()
                    val url = StringBuilder()
                        .append(view.context.getString(R.string.url_server))
                        .append(view.context.getString(R.string.recipeUpdateRecipeURL))
                        .append("?title=")
                        .append(recipe.title)
                        .append("&instructionsLink=")
                        .append(instructionsLink)
                        .append("&recipeID=")
                        .append(recipe.recipeID)
                        .append("&imageLink=")
                        .append(imageLink)
                        .append("&note=")
                        .append(recipe.note)
                        .append(ingredients)
                        .toString()
                    Log.d("recipe edit", "URL: $url")
                    val successListener = Response.Listener<JSONObject>
                    { response ->
                        try {
                            if (response != null) {
                                //get response
                                Log.d("recipe edit", "Response: $response")
                                callback(response.getBoolean("success"))
                            }
                        } catch (e: UnknownHostException) {
                            Log.d("recipe edit", "Unknown Host: ${e.message}")
                        }
                        catch (e: Exception) {
                            Log.d("recipe edit", "Response: ${e.message}")
                        }
                    }
                    val errorListener = Response.ErrorListener { error ->
                        Log.d("recipe edit", "Error Response: ${error.message}")
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
        }
    }

    fun editRecipeWithoutImage(
        ingredientsMutableList: MutableList<Ingredient>,
        instructionsArrayList: ArrayList<String>,
        recipe: Recipe,
        oldTitle: String,
        view: View,
        callback: (Boolean) -> Unit)
    {
        val ingredientsStringBuilder = StringBuilder()
        ingredientsMutableList.forEach {
            ingredientsStringBuilder.append("&ingredientIDArr[]=${it.ingredientId}")
        }
        val firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference

        val instructionsPathString = "recipeInstructions/${recipe.title}.txt"
        val instructionsUploadTask = storeTxtToFireBase(view, instructionsArrayList, instructionsPathString)

        instructionsUploadTask.addOnSuccessListener {
            firebaseStorageReference.child(instructionsPathString).downloadUrl.addOnSuccessListener {

                val instructionsLink = cleanedAccessToken(it.toString(), recipe.title, "txt")

                val imageLink = cleanedAccessToken(recipe.imageLink, oldTitle, "jpg")

                val ingredients = ingredientsStringBuilder.toString()
                val url = StringBuilder()
                    .append(view.context.getString(R.string.url_server))
                    .append(view.context.getString(R.string.recipeUpdateRecipeURL))
                    .append("?title=")
                    .append(recipe.title)
                    .append("&instructionsLink=")
                    .append(instructionsLink)
                    .append("&recipeID=")
                    .append(recipe.recipeID)
                    .append("&imageLink=")
                    .append(imageLink)
                    .append("&note=")
                    .append(recipe.note)
                    .append(ingredients)
                    .toString()
                Log.d("recipe edit", "URL: $url")
                val successListener = Response.Listener<JSONObject>
                { response ->
                    try {
                        if (response != null) {
                            //get response
                            Log.d("recipe edit", "Response: $response")
                            callback(response.getBoolean("success"))
                        }
                    } catch (e: UnknownHostException) {
                        Log.d("recipe edit", "Unknown Host: ${e.message}")
                    } catch (e: Exception) {
                        Log.d("recipe edit", "Response: ${e.message}")
                    }
                }
                val errorListener = Response.ErrorListener { error ->
                    Log.d("recipe edit", "Error Response: ${error.message}")
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
    }

    fun deleteRecipe(
        recipeID: String,
        isDeleted: Int,
        view: View,
        callback: (Boolean) -> Unit)
    {
        val url = StringBuilder()
            .append(view.context.getString(R.string.url_server))
            .append(view.context.getString(R.string.recipeDeleteRecipeURL))
            .append("?recipeID=")
            .append(recipeID)
            .append("&isDeleted=")
            .append(isDeleted)
            .toString()
        Log.d("recipe delete", "URL: $url")
        val successListener = Response.Listener<JSONObject>
        { response ->
            try {
                if (response != null) {
                    //get response
                    Log.d("recipe delete", "Response: $response")
                    callback(response.getBoolean("success"))
                }
            } catch (e: UnknownHostException) {
                Log.d("recipe delete", "Unknown Host: ${e.message}")
            }
            catch (e: Exception) {
                Log.d("recipe delete", "Response: ${e.message}")
            }
        }
        val errorListener = Response.ErrorListener { error ->
            Log.d("recipe delete", "Error Response: ${error.message}")
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

    fun createRecipe(
        userId: String,
        ingredientsMutableList: MutableList<Ingredient>,
        instructionsArrayList: ArrayList<String>,
        recipe: Recipe,
        imageUri: Uri,
        view: View,
        callback: (Boolean) -> Unit)
    {
        val ingredientsStringBuilder = StringBuilder()
        ingredientsMutableList.forEach {
            ingredientsStringBuilder.append("&ingredientIDArr[]=${it.ingredientId}")
        }
        val firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference

        val imagePathString = "recipeImage/${recipe.title}.jpg"
        val imageUploadTask = storeImageToFireBase(imagePathString, imageUri)

        val instructionsPathString = "recipeInstructions/${recipe.title}.txt"
        val instructionsUploadTask = storeTxtToFireBase(view, instructionsArrayList, instructionsPathString)

        instructionsUploadTask.addOnSuccessListener {
            imageUploadTask.addOnSuccessListener {
                val instructionURLTask = firebaseStorageReference.child(instructionsPathString).downloadUrl
                val imageURLTask = firebaseStorageReference.child(imagePathString).downloadUrl

                val getURLTask = Tasks.whenAllSuccess<Uri>(instructionURLTask, imageURLTask)
                getURLTask.addOnSuccessListener {

                    val instructionsLink = cleanedAccessToken(
                        it[0].toString(),
                        recipe.title,
                        "txt")

                    val imageLink = cleanedAccessToken(it[1].toString(), recipe.title, "jpg")

                    val ingredients = ingredientsStringBuilder.toString()
                    val url = StringBuilder()
                        .append(view.context.getString(R.string.url_server))
                        .append(view.context.getString(R.string.recipeCreateURL))
                        .append("?title=")
                        .append(recipe.title)
                        .append("&instructionsLink=")
                        .append(instructionsLink)
                        .append("&imageLink=")
                        .append(imageLink)
                        .append("&note=")
                        .append(recipe.note)
                        .append("&author=")
                        .append(userId)
                        .append(ingredients)
                        .toString()
                    Log.d("recipe create", "URL: $url")
                    val successListener = Response.Listener<JSONObject>
                    { response ->
                        try {
                            if (response != null) {
                                //get response
                                Log.d("recipe create", "Response: $response")
                                callback(response.getBoolean("success"))
                            }
                        } catch (e: UnknownHostException) {
                            Log.d("recipe create", "Unknown Host: ${e.message}")
                        }
                        catch (e: Exception) {
                            Log.d("recipe create", "Response: ${e.message}")
                        }
                    }
                    val errorListener = Response.ErrorListener { error ->
                        Log.d("recipe create", "Error Response: ${error.message}")
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
        }
    }

    fun convertXmlToPdf(view: View, displayMetrics: DisplayMetrics, fileName: String) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.EXACTLY)
        )
        Log.d("pdf", "Width Now " + view.measuredWidth)
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        // Create a new PdfDocument instance
        val document = PdfDocument()

        // Obtain the width and height of the view
        val viewWidth = view.measuredWidth
        val viewHeight = view.measuredHeight
        // Create a PageInfo object specifying the page attributes
        val pageInfo = PdfDocument.PageInfo.Builder(viewWidth, viewHeight, 1).create()

        // Start a new page
        val page = document.startPage(pageInfo)

        // Get the Canvas object to draw on the page
        val canvas = page.canvas

        // Create a Paint object for styling the view
        val paint = Paint()
        paint.color = Color.WHITE

        // Draw the view on the canvas
        view.draw(canvas)

        // Finish the page
        document.finishPage(page)

        // Specify the path and filename of the output PDF file
        val downloadsDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(downloadsDir, fileName)
        try {
            // Save the document to a file
            val fos = FileOutputStream(filePath)
            document.writeTo(fos)
            document.close()
            fos.close()
            // PDF conversion successful
            Toast.makeText(view.context, "Download successful", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            // Error occurred while converting to PDF
        }
    }


    private fun cleanedAccessToken(accessToken: String, title: String, fileType: String): String {
        val tokenLength = 36
        return java.lang.StringBuilder()
            .append(title)
            .append(".$fileType?alt=media%26token=")
            .append(accessToken.substring(accessToken.length-tokenLength, accessToken.length))
            .toString()
    }


    private fun storeTxtToFireBase(
        view: View,
        array: ArrayList<String>,
        pathString: String
    ): UploadTask {
        //create file
        File(view.context.filesDir, "temp.txt").bufferedWriter().use { out ->
            array.forEach {
                out.write("${it}\n")
            }
        }

        val firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference
        val storageRef = firebaseStorageReference.child(pathString)
        val uri = File(view.context.filesDir, "temp.txt").toUri()
        val metadata = storageMetadata {
            contentType = "text/plain"
        }

        //store to firebase
        return storageRef.putFile(uri, metadata)
    }

    private fun storeImageToFireBase(
        pathString: String,
        imageUri: Uri
    ): UploadTask {
        val firebaseStorageReference = FirebaseStorage.getInstance("gs://zeroxpire.appspot.com/").reference
        val storageRef = firebaseStorageReference.child(pathString)

        //store to firebase
        return storageRef.putFile(imageUri)
    }
}
