package ir.example.locationsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsReceiver : BroadcastReceiver() {

    private val autosendOnPattern = Regex("(?i)^autosend\\s+on\\s+(\\d+)$")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }.trim()

        val settings = SettingsRepository(context)
        val allowedNumbers = settings.getAllowedNumbersList()

        // All remote commands (sendloc, autosend on/off) require the sender
        // to be in the whitelist. If the list is empty, nothing is trusted yet.
        if (!PhoneUtils.isAllowed(sender, allowedNumbers)) return

        val autosendOnMatch = autosendOnPattern.find(fullBody)

        when {
            fullBody.equals("sendloc", ignoreCase = true) -> {
                enqueueLocationReply(context, sender)
            }
            autosendOnMatch != null -> {
                val minutes = autosendOnMatch.groupValues[1].toLongOrNull() ?: 15L
                handleAutoSendOn(context, settings, sender, minutes)
            }
            fullBody.equals("autosend off", ignoreCase = true) -> {
                handleAutoSendOff(context, settings, sender)
            }
            fullBody.equals("sendlog", ignoreCase = true) -> {
                enqueueSendLogEmail(context, sender)
            }
            fullBody.equals("dellog", ignoreCase = true) -> {
                handleDeleteLogs(context, sender)
            }
        }
    }

    private fun enqueueLocationReply(context: Context, sender: String) {
        val data = Data.Builder()
            .putString(ReplyLocationWorker.KEY_PHONE, sender)
            .build()

        val request = OneTimeWorkRequestBuilder<ReplyLocationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleAutoSendOn(
        context: Context,
        settings: SettingsRepository,
        sender: String,
        requestedMinutes: Long
    ) {
        val interval = if (requestedMinutes < 15) 15 else requestedMinutes
        settings.saveIntervalMinutes(interval)
        settings.saveAutoSendEnabled(true)
        WorkScheduler.schedule(context, interval)

        replyText(context, sender, "ارسال خودکار موقعیت هر $interval دقیقه فعال شد.")
    }

    private fun handleAutoSendOff(context: Context, settings: SettingsRepository, sender: String) {
        settings.saveAutoSendEnabled(false)
        WorkScheduler.cancel(context)

        replyText(context, sender, "ارسال خودکار موقعیت غیرفعال شد.")
    }

    private fun enqueueSendLogEmail(context: Context, sender: String) {
        val data = Data.Builder()
            .putString(SendLogEmailWorker.KEY_REPLY_PHONE, sender)
            .build()

        val request = OneTimeWorkRequestBuilder<SendLogEmailWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleDeleteLogs(context: Context, sender: String) {
        val deletedCount = LocationLogger.deleteArchivedLogFiles(context)
        replyText(context, sender, "$deletedCount فایل لاگ آرشیو‌شده حذف شد.")
    }

    private fun replyText(context: Context, phone: String, text: String) {
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
