package ir.example.locationsms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var titleText: TextView
    private lateinit var errorText: TextView
    private lateinit var submitButton: Button

    private var isSettingPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        settings = SettingsRepository(this)

        passwordInput = findViewById(R.id.passwordInput)
        confirmInput = findViewById(R.id.confirmInput)
        titleText = findViewById(R.id.loginTitle)
        errorText = findViewById(R.id.errorText)
        submitButton = findViewById(R.id.submitButton)

        isSettingPassword = !settings.isPasswordSet()

        if (isSettingPassword) {
            titleText.text = "برای ورود به برنامه یک رمز عبور تعیین کنید"
            confirmInput.visibility = android.view.View.VISIBLE
            submitButton.text = "تعیین رمز و ورود"
        } else {
            titleText.text = "برای ورود، رمز عبور را وارد کنید"
            confirmInput.visibility = android.view.View.GONE
            submitButton.text = "ورود"
        }

        submitButton.setOnClickListener { onSubmit() }
    }

    private fun onSubmit() {
        val password = passwordInput.text.toString()
        errorText.text = ""

        if (password.length < 4) {
            errorText.text = "رمز عبور باید حداقل ۴ کاراکتر باشد"
            return
        }

        if (isSettingPassword) {
            val confirm = confirmInput.text.toString()
            if (password != confirm) {
                errorText.text = "تکرار رمز عبور مطابقت ندارد"
                return
            }
            settings.setPassword(password)
            proceedToMain()
        } else {
            if (settings.verifyPassword(password)) {
                proceedToMain()
            } else {
                errorText.text = "رمز عبور اشتباه است"
                passwordInput.text?.clear()
            }
        }
    }

    private fun proceedToMain() {
        AppLockState.unlocked = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
