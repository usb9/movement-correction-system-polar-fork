package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val trainingButton = findViewById<Button>(R.id.no_need_account_button)

        trainingButton.setOnClickListener {
            val nextPage = Intent(this, HomeActivity::class.java)
            startActivity(nextPage)
            finish()
        }
    }
}