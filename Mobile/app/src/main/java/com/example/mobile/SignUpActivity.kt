package com.example.mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class SignUpActivity : AppCompatActivity() {
    private lateinit var signupButton: Button
    private lateinit var emailText: TextView
    private lateinit var passwordText: TextView
    private lateinit var usernameText: TextView
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        auth = Firebase.auth

        //set value from text View
        signupButton=findViewById(R.id.button_signup)
        emailText=findViewById(R.id.email_signup)
        passwordText=findViewById(R.id.password_signup)
        usernameText=findViewById(R.id.username_signup)


        //navigation
        val backArrow = findViewById<TextView>(R.id.signup_nav_bar)
        backArrow.setOnClickListener {
            val signInpage = Intent(this, SignInActivity::class.java)
            startActivity(signInpage)
            finish()
        }


        signupButton.setOnClickListener{
            if ( emailText.text.toString().isNotEmpty()
                && passwordText.text.toString().isNotEmpty()
                && usernameText.text.toString().isNotEmpty()) {
                createAccount(
                    emailText.text.toString(),
                    passwordText.text.toString(),
                    usernameText.text.toString()
                )
            }else {
                Toast.makeText(this, "Empty fields are not Allowed !", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun updateUI() {
        val intent = Intent(this,MainActivity::class.java)
        intent.putExtra("Login",true)
        startActivity(intent)
    }
    private fun createAccount(email: String, password: String, username: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "createUserWithEmail:success")
                    val user = auth.currentUser


                    user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build())
                    updateUI()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        // [END create_user_with_email]
    }
}