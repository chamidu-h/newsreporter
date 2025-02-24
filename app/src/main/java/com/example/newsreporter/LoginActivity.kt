package com.example.newsreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText


class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var resetPasswordText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        UserSessionManager.init(applicationContext)
        setupToolbar()
        initializeViews()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.editTextEmail)
        passwordInput = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        resetPasswordText = findViewById(R.id.textViewResetPassword)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        resetPasswordText.setOnClickListener {
            // Handle password reset
            Toast.makeText(this, "Password reset functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        return true
    }

    private fun performLogin() {
        loginButton.isEnabled = false
        loginButton.text = "Signing in..."

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val loginRequest = LoginRequest(email, password)

        val call: Call<LoginResponse> = RetrofitClient.authApi.login(loginRequest)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                loginButton.isEnabled = true
                loginButton.text = "Sign In"

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.success) {
                        handleSuccessfulLogin(loginResponse)
                    } else {
                        showError(loginResponse.message ?: "Login failed")
                    }
                } else {
                    showError("Invalid server response")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                loginButton.isEnabled = true
                loginButton.text = "Sign In"
                showError(t.localizedMessage ?: "Network error occurred")
            }
        })
    }

    private fun handleSuccessfulLogin(loginResponse: LoginResponse) {
        UserSessionManager.saveUserId(loginResponse.userId!!)

        val successSnackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "Login successful",
            Snackbar.LENGTH_SHORT
        )
        successSnackbar.setBackgroundTint(resources.getColor(R.color.teal_700))
        successSnackbar.show()

        // Navigate to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }, 1000)
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(resources.getColor(android.R.color.holo_red_dark))
            .show()
    }
}
