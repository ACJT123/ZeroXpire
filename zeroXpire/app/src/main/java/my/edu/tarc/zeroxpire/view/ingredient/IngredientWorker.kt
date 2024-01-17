package my.edu.tarc.zeroxpire.view.ingredient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.auth.FirebaseAuth
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.WebDB
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*

class IngredientWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val appContext = context.applicationContext

    companion object {
        const val CHANNEL_ID = "expiry_channel"
    }

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()

        // Check if the work has already been done
        val sharedPreferences = appContext.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val isWorkDone = sharedPreferences.getBoolean("isIngredientWorkDone", false)

        if (!isWorkDone) {
            loadIngredient(auth)

            // Mark the work as done
            sharedPreferences.edit().putBoolean("isIngredientWorkDone", true).apply()
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Expiration Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for expiring ingredients"
            }

            val notificationManager = appContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendExpiryNotification(countExpiredInFiveDays: Int) {
        val notificationId = 1

        val notificationBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.final_logo)
            .setContentTitle("Ingredients Expiring Soon")
            .setContentText("$countExpiredInFiveDays ingredients will expire in the next 5 days.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(appContext)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun loadIngredient(auth: FirebaseAuth) {
        val url: String = appContext.getString(R.string.url_server) + appContext.getString(R.string.url_read_ingredient) + "?userId=${auth.currentUser?.uid}"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val currentDate = Calendar.getInstance().time
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, 5)
                        val fiveDaysLater = calendar.time
                        var countExpiredInFiveDays = 0

                        if (jsonArray.length() > 0) {
                            for (i in 0 until jsonArray.length()) {
                                val jsonIngredient: JSONObject = jsonArray.getJSONObject(i)
                                val expiryDateString = jsonIngredient.getString("expiryDate")
                                val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDateString)

                                if (expiryDate != null && expiryDate >= currentDate && expiryDate <= fiveDaysLater) {
                                    countExpiredInFiveDays++
                                }
                            }
                        }

                        Log.d("ExpiredInFiveDays", "Count: $countExpiredInFiveDays")

                        if (countExpiredInFiveDays > 0) {
                            createNotificationChannel()
                            sendExpiryNotification(countExpiredInFiveDays)
                        }
                    }
                } catch (e: UnknownHostException) {
                    Log.d("ContactRepository", "Unknown Host: ${e.message}")
                } catch (e: Exception) {
                    Log.d("Cannot load", "Response: ${e.message}")
                }
            },
            { error ->
                // Handle the error case
                // ...
            }
        )

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0,
            1f
        )

        WebDB.getInstance(appContext).addToRequestQueue(jsonObjectRequest)
    }
}
