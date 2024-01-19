package aa.studio.storywritter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)
        var btnRegister: Button = findViewById(R.id.btnSignUp)
        btnRegister.setOnClickListener(View.OnClickListener {
            var username: EditText = findViewById(R.id.editTextUsername)
            var password: EditText = findViewById(R.id.editTextPassword)
            var usernameText:String = username.text.toString()
            var passwordText:String = password.text.toString()
            registerUser(usernameText, passwordText)
        })
        var btnnavigate : Button = findViewById(R.id.btnNavigate)
        btnnavigate.setOnClickListener(View.OnClickListener {
            var intent: Intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        })
    }
    private fun registerUser(email: String, password: String) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, object : OnCompleteListener<AuthResult?> {
                override fun onComplete(task: Task<AuthResult?>) {
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user: FirebaseUser? = auth.currentUser
                        if (user != null) {
                            Log.d("Registration", user.email + " successfully Registered.")
                        }
                        Toast.makeText(this@SignUpActivity, "Registration successful.", Toast.LENGTH_SHORT).show()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e("Registration", "failure", task.exception)
                        Toast.makeText(this@SignUpActivity, "Registration failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }
}