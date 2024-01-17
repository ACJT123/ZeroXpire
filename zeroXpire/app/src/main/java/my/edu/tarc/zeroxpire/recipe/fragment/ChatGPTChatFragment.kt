package my.edu.tarc.zeroxpire.recipe.fragment

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.cdimascio.dotenv.dotenv
import my.edu.tarc.zeroxpire.R
import my.edu.tarc.zeroxpire.recipe.Utilities
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ChatGPTChatFragment : Fragment() {
    private val client = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS) // Adjust the timeout duration as needed
        .readTimeout(15, TimeUnit.SECONDS)    // Adjust the timeout duration as needed
        .build()

    private lateinit var currentView: View
    private val utilities = Utilities()

    private var responseIndex = 0

    private val stringBuilderArrayList = ArrayList<StringBuilder>()
    private val chatGPTChatBoxArrayList = ArrayList<LinearLayout>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        disableBtmNav()
        currentView = inflater.inflate(R.layout.fragment_chatgpt_chat, container, false)

        // Load the API key from the .env file in the root directory
        val dotenv = dotenv {
            directory = "/assets"
            filename = "env" // instead of '.env', use 'env'
        }

        val args: ChatGPTChatFragmentArgs by navArgs()
        val recipeName = args.recipeName
        val ingredientNameArrayList = args.ingredientNameArrayList

        val key = dotenv["OPENAI_TOKEN"]

        val questionEditText = currentView.findViewById<EditText>(R.id.questionEditText)
        val sendImageView = currentView.findViewById<ImageView>(R.id.sendImageView)
        val upBtn = currentView.findViewById<ImageView>(R.id.upBtn)
        val chatLinearLayout = currentView.findViewById<LinearLayout>(R.id.chatLinearLayout)

        upBtn.setOnClickListener {
            findNavController().popBackStack()
        }


        val initialText =
            if (ingredientNameArrayList.isEmpty()) {
                "Create a $recipeName recipe"
            }else {
                val ingredientsToUse = ingredientNameArrayList.joinToString(separator = ", ")
                "Create a $recipeName recipe using $ingredientsToUse"
            }
        questionEditText.setText(initialText)

        questionEditText.doOnTextChanged { text, _, _, _ ->
            sendImageView.isEnabled = !text.isNullOrBlank()
            sendImageView.isClickable = !text.isNullOrBlank()
        }

        sendImageView.setOnClickListener {
            //user input
            val input = questionEditText.text.toString()
            Log.d("User ask chatGPT", "question: $input")

            questionEditText.setText("")

            val userChat = utilities.createNewChat(currentView, input, "You", Gravity.END)
            chatLinearLayout.addView(userChat)

            askChatGPT(responseIndex, input, key) {
                //chatGPT response
                if (it.second) {
                    //if is new response, create new chatBox
                    //else add to existing chatBox
                    if (it.first >= responseIndex) {
                        responseIndex++
                        activity!!.runOnUiThread {
                            val chatGPTChat = utilities.createNewChat(
                                currentView,
                                it.third,
                                "ChatGPT",
                                Gravity.START
                            )
                            chatLinearLayout.addView(chatGPTChat)
                            chatGPTChatBoxArrayList.add(chatGPTChat)
                        }
                        stringBuilderArrayList.add(StringBuilder(it.third))
                    }else {
                        while (it.first >= chatGPTChatBoxArrayList.size){
                            Log.d("waiting for main UI Thread", "index: ${it.first}, size: ${chatGPTChatBoxArrayList.size}")
                        }

                        val chatBoxLinearLayout = chatGPTChatBoxArrayList[it.first]
                        val cardView = chatBoxLinearLayout[0] as CardView
                        val chatContentLinearLayout = cardView[0] as LinearLayout
                        val chat = chatContentLinearLayout[1] as TextView

                        stringBuilderArrayList[it.first].append(it.third)
                        activity!!.runOnUiThread {
                            chat.text = stringBuilderArrayList[it.first]
                        }
                    }
                }else {
                    activity!!.runOnUiThread { Toast.makeText(currentView.context, "An error occurred", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        return currentView
    }

    private fun askChatGPT(index: Int, input: String, key: String, callback: (Triple<Int, Boolean, String>) -> Unit) {
        var finish = false
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBody="""
            {
             "model": "gpt-3.5-turbo",
             "messages": [{"role": "user", "content": "$input"}],
             "temperature": 0.7,
             "stream": true
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $key")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("error", "Api failed: $e")
                callback(Triple(index, false, ""))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.d("error", "Api failed with code: ${response.code}")
                    Log.d("error", "Api failed response: $response")
                    callback(Triple(index, false, ""))
                    return
                }

                val responseBody = response.body
                if (responseBody != null) {
                    val inputStream = responseBody.byteStream()
                    val stringBuilder = StringBuilder()

                    try {
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        var line: String? = reader.readLine()

                        while (!finish) {
                            Log.d("chatGPT Response buffer", line!!)
                            line = line.substringAfter("data: ")
                            Log.d("chatGPT Response buffer after substring", line)
                            if (line.isNotBlank()) {
                                val jsonObject = JSONObject(line)
                                val choices = jsonObject.getJSONArray("choices").getJSONObject(0)

                                val finishReason = choices.getString("finish_reason")
                                Log.d("GPT finish reason", finishReason)
                                if (finishReason.equals("stop")) {
                                    finish = true
                                    break
                                }

                                val delta = choices.getJSONObject("delta")
                                val content = delta.getString("content")
                                Log.d("GPT JSON string", content)
                                callback(Triple(index, true, content))

                            }

                            stringBuilder.append(line)
                            line = reader.readLine()
                        }

//                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                            Log.d("chatGPT Response buffer",  String(buffer))
//                            val chunk = String(buffer, 0, bytesRead)
//                            chatGPTRespond(true, chunk)
//                            if (!finish){
////                                Log.d("chatGPT Response", chunk)
////                                val jsonObject = JSONObject(chunk)
////                                val data = jsonObject.getJSONObject("data")
////                                val choices = data.getJSONArray("choices").getJSONObject(0)
////                                val delta = choices.getJSONObject("delta")
////                                val content = delta.getString("content")
////                                val finishReason = choices.getString("finish_reason")
////                                Log.d("GPT JSON string", content)
////                                Log.d("GPT finish reason", finishReason)
//                                callback(Pair(true, chunk))
////
////                                if (!finishReason.isNullOrBlank()) {
////                                    finish = true
////                                }
//                            }
//
//                            stringBuilder.append(chunk)
//                        }
                    } catch (e: IOException) {
                        Log.e("error", "Error reading response stream", e)
                        callback(Triple(index, false, ""))
                        return
                    } finally {
                        inputStream.close()
                        responseBody.close()
                    }
                } else {
                    Log.d("chatGPT Response", "empty")
                    callback(Triple(index, false, "empty"))
                }


//            override fun onResponse(call: Call, response: Response) {
//                val body = response.body?.string()
//                Log.d("data", "$body")
//                if (body != null) {
//                    Log.d("chatGPT Response", body)
//                }else {
//                    Log.d("chatGPT Response", "empty")
//                    callback(Pair(false, "empty"))
//                    return
//                }
//
//                val jsonObject = JSONObject(body)
//                val jsonArray: JSONArray=jsonObject.getJSONArray("choices")
//                Log.d("chatGPT JSONArray", "$jsonArray")
//                val text = jsonArray.getJSONObject(0).getJSONObject("message")
//                    .getString("content")
//                Log.d("chatGPT content", "content: $text")
//                callback(Pair(true, text))
            }

        })
    }

    private fun disableBtmNav() {
        val view = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)
        view.visibility = View.INVISIBLE

        val add = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
        add.visibility = View.INVISIBLE
    }
}