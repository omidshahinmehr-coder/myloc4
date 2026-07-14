package ir.example.locationsms

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendLogEmailWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_REPLY_PHONE = "reply_phone"
    }

    override suspend fun doWork(): Result {
        val replyPhone = inputData.getString(KEY_REPLY_PHONE)

        if (!hasInternetConnection(applicationContext)) {
            replyPhone?.let {
                sendReply(applicationContext, it, "اتصال اینترنت برقرار نیست؛ ارسال فایل لاگ به ایمیل انجام نشد.")
            }
            return Result.success()
        }

        val settings = SettingsRepository(applicationContext)
        val senderEmail = settings.getSenderEmail()
        val senderPassword = settings.getSenderEmailPassword()
        val recipientEmail = settings.getRecipientEmail()
        val smtpHost = settings.getSmtpHost()
        val smtpPort = settings.getSmtpPort()

        if (senderEmail.isNullOrBlank() || senderPassword.isNullOrBlank() || recipientEmail.isNullOrBlank()) {
            replyPhone?.let {
                sendReply(applicationContext, it, "تنظیمات ایمیل کامل نیست؛ ابتدا از داخل اپ ایمیل‌ها را تنظیم کنید.")
            }
            return Result.success()
        }

        val files = mutableListOf<java.io.File>()
        val currentFile = LocationLogger.getLogFile(applicationContext)
        if (currentFile.exists()) files.add(currentFile)
        files.addAll(LocationLogger.getArchivedLogFiles(applicationContext))

        if (files.isEmpty()) {
            replyPhone?.let {
                sendReply(applicationContext, it, "فایل لاگی برای ارسال وجود ندارد.")
            }
            return Result.success()
        }

        val success = withContext(Dispatchers.IO) {
            EmailSender.sendLogEmail(
                smtpHost = smtpHost,
                smtpPort = smtpPort,
                senderEmail = senderEmail,
                senderPassword = senderPassword,
                recipientEmail = recipientEmail,
                subject = "My Car Tracker - فایل لاگ موقعیت مکانی",
                bodyText = "فایل(های) لاگ موقعیت مکانی این خودرو پیوست شده است.",
                attachmentFiles = files
            )
        }

        replyPhone?.let {
            val message = if (success) {
                "فایل لاگ با موفقیت به ایمیل ارسال شد."
            } else {
                "ارسال فایل لاگ به ایمیل با خطا مواجه شد. تنظیمات ایمیل را بررسی کنید."
            }
            sendReply(applicationContext, it, message)
        }

        return Result.success()
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun sendReply(context: Context, phone: String, text: String) {
        val data = Data.Builder()
            .putString(ReplyTextWorker.KEY_PHONE, phone)
            .putString(ReplyTextWorker.KEY_TEXT, text)
            .build()

        val request = OneTimeWorkRequestBuilder<ReplyTextWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
