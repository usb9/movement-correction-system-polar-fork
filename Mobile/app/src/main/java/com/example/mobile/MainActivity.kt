package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        var logged: Boolean = intent.getBooleanExtra("Login",false)
        var noNeedAccount: Boolean = intent.getBooleanExtra("isNoAccount",false)

        if (logged || noNeedAccount) {
            val nextPage = Intent(this, HomeActivity::class.java)
            startActivity(nextPage)
        } else {
            val nextPage = Intent(this, SignInActivity::class.java)
            startActivity(nextPage)
        }

        finish()
    }

}