package com.example.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private val fileName: String = "DoINeedAccount.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var logged: Boolean = intent.getBooleanExtra("Login",false)
        var noNeedAccount: Boolean = false

        // check no need account status
        try {
            var fin: FileInputStream? = null
            fin = openFileInput(fileName)
            var inputStreamReader: InputStreamReader = InputStreamReader(fin)
            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)

            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
                text?.let {
                    noNeedAccount = it.contains("no")
                }
            }
        } catch (ex: Exception) {
            if (ex.message?.contains("No such file or directory") == true) {
                noNeedAccount = false
            }
        }

        // navigation next screen
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