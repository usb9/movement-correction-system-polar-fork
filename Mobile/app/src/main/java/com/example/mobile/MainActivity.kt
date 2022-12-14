package com.example.mobile

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*


class MainActivity : AppCompatActivity() {
    private val fileName: String = "DoINeedAccount.txt"
    private val dataFileName: String = "database.txt"

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



//        var string = "training,1,Tue Dec 13 02:29:32 GMT+01:00 2022\n" +
//                "round,1,1\n" +
//                "punch,1,false,10.00\n" +
//                "punch,1,true,11.00\n" +
//                "round_info,1,200,2,1,1,20.0,10.5\n"+
//                "round,1,2\n" +
//                "punch,2,false,10.00\n" +
//                "punch,2,true,11.00\n" +
//                "round_info,2,200,2,1,1,20.0,10.5\n"
//
//        Log.e("TAG",string)
//
//        try {
//            val bufferedWriter =
//                BufferedWriter(FileWriter(File(filesDir.toString() + File.separator + dataFileName)))
//            bufferedWriter.write(string)
//            bufferedWriter.close()
//        } catch (ex: Exception) {
//            //Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
//            Log.e("hihihaha", "Error: ${ex.message}")
//        }
        val dataHandler = DataReader()

        dataHandler.DataHandler()



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