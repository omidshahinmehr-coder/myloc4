package ir.example.locationsms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var phoneInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var allowedNumbersInput: EditText
    private lateinit var logIntervalInput: EditText
    private lateinit var recipientEmailInput: EditText
    private lateinit var senderEmailInput: EditText
    private lateinit var senderEmailPasswordInput: EditText
    private lateinit var smtpHostInput: EditText
    private lateinit var smtpPortInput: EditText
    private lateinit var autoSendSwitch: Switch
    private lateinit var statusText: TextView

    private val basePermissions: Array<String> by lazy {
        val list = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                requestBackgroundLocationIfNeeded()
            } else {
                statusText.text = "برخی مجوزها داده نشد. بدون آن‌ها برنامه به‌درستی کار نخواهد کرد."
            }
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Whether granted or not, proceed — foreground-only will still work
            // for the sendloc reply while the app is running.
            finishSetup()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsRepository(this)
        phoneInput = findViewById(R.id.phoneInput)
        intervalInput = findViewById(R.id.intervalInput)
        allowedNumbersInput = findViewById(R.id.allowedNumbersInput)
        logIntervalInput = findViewById(R.id.logIntervalInput)
        recipientEmailInput = findViewById(R.id.recipientEmailInput)
        senderEmailInput = findViewById(R.id.senderEmailInput)
        senderEmailPasswordInput = findViewById(R.id.senderEmailPasswordInput)
        smtpHostInput = findViewById(R.id.smtpHostInput)
        smtpPortInput = findViewById(R.id.smtpPortInput)
        autoSendSwitch = findViewById(R.id.autoSendSwitch)
        statusText = findViewById(R.id.statusText)

        phoneInput.setText(settings.getPhoneNumber() ?: "")
        intervalInput.setText(settings.getIntervalMinutes().toString())
        allowedNumbersInput.setText(settings.getAllowedNumbersRaw())
        logIntervalInput.setText(settings.getLogIntervalMinutes().toString())
        recipientEmailInput.setText(settings.getRecipientEmail() ?: "")
        senderEmailInput.setText(settings.getSenderEmail() ?: "")
        senderEmailPasswordInput.setText(settings.getSenderEmailPassword() ?: "")
        smtpHostInput.setText(settings.getSmtpHost())
        smtpPortInput.setText(settings.getSmtpPort())
        autoSendSwitch.isChecked = settings.isAutoSendEnabled()

        findViewById<Button>(R.id.saveButton).setOnClickListener { onSaveClicked() }
    }

    override fun onStart() {
        super.onStart()
        if (!AppLockState.unlocked) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        // Require the password again the next time the app is opened
        // (including from Recents), not just after a full process restart.
        AppLockState.unlocked = false
    }

    private fun onSaveClicked() {
        val phone = phoneInput.text.toString().trim()
        val intervalText = intervalInput.text.toString().trim()
        val allowedRaw = allowedNumbersInput.text.toString().trim()
        val logIntervalText = logIntervalInput.text.toString().trim()
        val recipientEmail = recipientEmailInput.text.toString().trim()
        val senderEmail = senderEmailInput.text.toString().trim()
        val senderEmailPassword = senderEmailPasswordInput.text.toString().trim()
        val smtpHost = smtpHostInput.text.toString().trim().ifEmpty { "smtp.gmail.com" }
        val smtpPort = smtpPortInput.text.toString().trim().ifEmpty { "587" }

        if (autoSendSwitch.isChecked && phone.isEmpty()) {
            Toast.makeText(this, "برای ارسال خودکار، شماره تلفن را وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val interval = intervalText.toLongOrNull() ?: 15L
        val safeInterval = if (interval < 15) 15L else interval

        val logInterval = logIntervalText.toLongOrNull() ?: 60L
        val safeLogInterval = if (logInterval < 15) 15L else logInterval

        if (interval < 15 || logInterval < 15) {
            Toast.makeText(this, "حداقل بازه مجاز ۱۵ دقیقه است", Toast.LENGTH_SHORT).show()
        }

        settings.savePhoneNumber(phone)
        settings.saveIntervalMinutes(safeInterval)
        settings.saveAllowedNumbersRaw(allowedRaw)
        settings.saveAutoSendEnabled(autoSendSwitch.isChecked)
        settings.saveLogIntervalMinutes(safeLogInterval)
        settings.saveRecipientEmail(recipientEmail)
        settings.saveSenderEmail(senderEmail)
        settings.saveSenderEmailPassword(senderEmailPassword)
        settings.saveSmtpHost(smtpHost)
        settings.saveSmtpPort(smtpPort)

        intervalInput.setText(safeInterval.toString())
        logIntervalInput.setText(safeLogInterval.toString())

        if (settings.getAllowedNumbersList().isEmpty()) {
            Toast.makeText(
                this,
                "توجه: چون هیچ شماره مجازی ثبت نکردید، دستورات پیامکی (sendloc, autosend, sendlog, dellog) پاسخ داده نمی‌شوند",
                Toast.LENGTH_LONG
            ).show()
        }

        if (!hasAllBasePermissions()) {
            permissionLauncher.launch(basePermissions)
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun hasAllBasePermissions(): Boolean =
        basePermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return
            }
        }
        finishSetup()
    }

    private fun finishSetup() {
        // The location-log timer always runs, independent of the SMS
        // auto-send switch.
        LogWorkScheduler.schedule(this, settings.getLogIntervalMinutes())

        if (autoSendSwitch.isChecked) {
            WorkScheduler.schedule(this, settings.getIntervalMinutes())
            statusText.text =
                "تنظیمات ذخیره شد. ارسال خودکار پیامکی هر ${settings.getIntervalMinutes()} دقیقه فعال است. " +
                "ذخیره در فایل هر ${settings.getLogIntervalMinutes()} دقیقه ادامه دارد."
        } else {
            WorkScheduler.cancel(this)
            statusText.text =
                "تنظیمات ذخیره شد. ارسال خودکار پیامکی غیرفعال است، اما ذخیره در فایل هر ${settings.getLogIntervalMinutes()} دقیقه ادامه دارد.\n" +
                "برای دریافت موقعیت فوری 'sendloc'، ارسال فایل لاگ به ایمیل 'sendlog'، یا حذف آرشیو 'dellog' بفرستید."
        }
    }
}
