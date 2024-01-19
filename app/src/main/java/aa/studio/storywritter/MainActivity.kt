package aa.studio.storywritter

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var titleDescriptionEditText: EditText
    private lateinit var generateStoryButton: Button
    private lateinit var storyTextView: TextView

    private val apiKey = "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"
    private val apiUrl = "https://api.openai.com/v1/engines/gpt-3.5-turbo-instruct/completions"

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        titleDescriptionEditText = findViewById(R.id.titleDescriptionEditText)
        generateStoryButton = findViewById(R.id.generateStoryButton)
        storyTextView = findViewById(R.id.storyTextView)

        generateStoryButton.setOnClickListener {
            val inputText = titleDescriptionEditText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                generateStory(inputText)
            }
        }
    }

    private fun generateStory(inputText: String, retryCount: Int = 0) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val prompt = "Generate a story related to: $inputText\n\nOnce upon a time,"
                val temperature = 0.7

                val jsonBody = JSONObject()
                    .put("prompt", prompt)
                    .put("temperature", temperature)
                    .put("max_tokens", 1500)

                val requestBody = RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    jsonBody.toString()
                )

                val response = OkHttpClient().newCall(
                    Request.Builder()
                        .url(apiUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .build()
                ).execute()

                withContext(Dispatchers.Main) {
                    handleResponse(response, inputText)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("NETWORK_ERROR", "Error fetching story: $e")
                if (retryCount < MAX_RETRY_COUNT) {
                    Log.d("RETRY", "Retrying after 10 seconds...")
                    delay(RETRY_DELAY_MILLIS)
                    generateStory(inputText, retryCount + 1)
                } else {
                    withContext(Dispatchers.Main) {
                        updateUI("Error fetching story. Retry limit reached.")
                    }
                }
            }
        }
    }

    private fun handleResponse(response: okhttp3.Response, title: String) {
        if (response.isSuccessful) {
            val result = response.body?.string()
            Log.d("API_RESPONSE", result ?: "Empty response")

            parseStory(result)?.let { storyText ->
                updateUI(storyText)
                printToLogcat(storyText)

                // Save the story to Firebase Realtime Database
                saveStoryToFirebase(title, storyText)
            } ?: updateUI("No story available")
        } else {
            val errorBody = response.body?.string()
            Log.e("API_ERROR", "Error: ${response.code} - ${response.message}\n$errorBody")

            updateUI("Error: ${response.code} - ${response.message}\n$errorBody")
        }
    }

    private fun parseStory(responseBody: String?): String? {
        return try {
            val json = JSONObject(responseBody)
            val choicesArray = json.getJSONArray("choices")

            if (choicesArray.length() > 0) {
                choicesArray.getJSONObject(0).optString("text", "")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateUI(storyText: String) {
        val formattedStory = storyText.capitalize()
        storyTextView.text = formattedStory
    }

    private fun printToLogcat(storyText: String) {
        Log.d("FETCHED_STORY", storyText)
    }

    private fun saveStoryToFirebase(title: String, story: String) {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email
            if (userEmail != null) {
                val storyData = mapOf(
                    "title" to title,
                    "story" to story
                )

                database.reference.child("stories").child(userEmail.replace(".", "_")).setValue(storyData)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FIREBASE", "Story saved to Firebase")
                            Toast.makeText(this@MainActivity, "Story Saved in DataBase!!.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("FIREBASE", "Error saving story to Firebase: ${task.exception}")
                            Toast.makeText(this@MainActivity, "Failed to store story!!!.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MILLIS = 10000L
    }
}
