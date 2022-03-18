package com.supertiger.nertivia.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.supertiger.nertivia.R
import com.supertiger.nertivia.SharedPreference
import com.supertiger.nertivia.cache.token
import com.supertiger.nertivia.models.LoginData
import com.supertiger.nertivia.models.LoginResponse
import com.supertiger.nertivia.models.RegisterDeviceData
import com.supertiger.nertivia.services.ServiceBuilder
import com.supertiger.nertivia.services.UserService
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import javax.security.auth.callback.Callback


class LoginActivity : AppCompatActivity() {
    private val userService = ServiceBuilder.buildService(UserService::class.java)
    private var sharedPreference: SharedPreference? = null
    var gson = Gson()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sharedPreference= SharedPreference(this)
        // Login button event
        loginBtn.setOnClickListener { login() }

    }

    private fun login () {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val loginData = LoginData(email, password)

        val requestCall = userService.login(loginData)

        requestCall.enqueue(object: Callback, retrofit2.Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(applicationContext,  t.message, Toast.LENGTH_SHORT).show()
            }
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val responseToken = response.body()?.token
                    token = responseToken

                    val user = gson.toJson(response.body()?.user)

                    sharedPreference?.save("token", responseToken.toString())
                    sharedPreference?.save("user", user.toString())
                    // go to main activity after logged in.
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()

                    Toast.makeText(applicationContext, "Logged In" , Toast.LENGTH_SHORT).show()
                    // FIREBASE CLOUD NOTIFICATIONS
                    setupFirebase()
                } else {
                    val jObjError = JSONObject(response.errorBody()?.string())
                    val errors = jObjError.getJSONArray("errors")
                    val firstError = errors.getJSONObject(0).get("msg")
                    Toast.makeText(applicationContext, firstError.toString() , Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupFirebase() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener {task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // Get new Instance ID token

            val token = task.result?.token

            userService.registerDevice(RegisterDeviceData(token)).enqueue(object: Callback, retrofit2.Callback<Any?> {
                override fun onFailure(call: Call<Any?>, t: Throwable) {
                    Toast.makeText(applicationContext,  t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                    if (response.isSuccessful) {
                        Toast.makeText(    applicationContext,  "success",     Toast.LENGTH_SHORT   ).show()
                    } else {
                        //val jObjError = JSONObject(response.errorBody()?.string())
                        Toast.makeText(    applicationContext,  "fail ;c",     Toast.LENGTH_SHORT   ).show()
                    }
                }

            })

        })
    }
}