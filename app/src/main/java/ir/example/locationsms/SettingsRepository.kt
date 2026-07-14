package ir.example.locationsms

import android.content.Context
import android.util.Base64

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("location_sms_prefs", Context.MODE_PRIVATE)

    fun savePhoneNumber(phone: String) {
        prefs.edit().putString(KEY_PHONE, phone).apply()
    }

    fun getPhoneNumber(): String? = prefs.getString(KEY_PHONE, null)

    fun saveIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_INTERVAL, minutes).apply()
    }

    fun getIntervalMinutes(): Long = prefs.getLong(KEY_INTERVAL, 60L)

    fun saveAutoSendEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isAutoSendEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    // --- Allowed sender numbers (whitelist for "sendloc") ---

    fun saveAllowedNumbersRaw(raw: String) {
        prefs.edit().putString(KEY_ALLOWED, raw).apply()
    }

    fun getAllowedNumbersRaw(): String = prefs.getString(KEY_ALLOWED, "") ?: ""

    fun getAllowedNumbersList(): List<String> =
        getAllowedNumbersRaw()
            .split(",", "،", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    // --- App-open password ---

    fun isPasswordSet(): Boolean = prefs.contains(KEY_PASS_HASH)

    fun setPassword(password: String) {
        val salt = PasswordUtils.generateSalt()
        val hash = PasswordUtils.hash(password, salt)
        prefs.edit()
            .putString(KEY_PASS_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASS_HASH, hash)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val saltB64 = prefs.getString(KEY_PASS_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PASS_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hash = PasswordUtils.hash(password, salt)
        return hash == storedHash
    }

    // --- Independent location-log timer ---

    fun saveLogIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_LOG_INTERVAL, minutes).apply()
    }

    fun getLogIntervalMinutes(): Long = prefs.getLong(KEY_LOG_INTERVAL, 60L)

    /** 0 means "no active log file yet" (used to detect when to start counting for rotation). */
    fun saveLogStartTime(millis: Long) {
        prefs.edit().putLong(KEY_LOG_START, millis).apply()
    }

    fun getLogStartTime(): Long = prefs.getLong(KEY_LOG_START, 0L)

    // --- Email / SMTP settings (used by the "sendlog" command) ---

    fun saveRecipientEmail(email: String) {
        prefs.edit().putString(KEY_RECIPIENT_EMAIL, email).apply()
    }

    fun getRecipientEmail(): String? = prefs.getString(KEY_RECIPIENT_EMAIL, null)

    fun saveSenderEmail(email: String) {
        prefs.edit().putString(KEY_SENDER_EMAIL, email).apply()
    }

    fun getSenderEmail(): String? = prefs.getString(KEY_SENDER_EMAIL, null)

    fun saveSenderEmailPassword(password: String) {
        prefs.edit().putString(KEY_SENDER_EMAIL_PASSWORD, password).apply()
    }

    fun getSenderEmailPassword(): String? = prefs.getString(KEY_SENDER_EMAIL_PASSWORD, null)

    fun saveSmtpHost(host: String) {
        prefs.edit().putString(KEY_SMTP_HOST, host).apply()
    }

    fun getSmtpHost(): String = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"

    fun saveSmtpPort(port: String) {
        prefs.edit().putString(KEY_SMTP_PORT, port).apply()
    }

    fun getSmtpPort(): String = prefs.getString(KEY_SMTP_PORT, "587") ?: "587"

    companion object {
        private const val KEY_PHONE = "phone_number"
        private const val KEY_INTERVAL = "interval_minutes"
        private const val KEY_ENABLED = "auto_send_enabled"
        private const val KEY_ALLOWED = "allowed_numbers"
        private const val KEY_PASS_SALT = "pass_salt"
        private const val KEY_PASS_HASH = "pass_hash"
        private const val KEY_LOG_INTERVAL = "log_interval_minutes"
        private const val KEY_LOG_START = "log_start_time"
        private const val KEY_RECIPIENT_EMAIL = "recipient_email"
        private const val KEY_SENDER_EMAIL = "sender_email"
        private const val KEY_SENDER_EMAIL_PASSWORD = "sender_email_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
    }
}
