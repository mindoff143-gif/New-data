package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.data.database.AssistantDatabase
import com.example.data.model.CommunicationLogItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsBroadcastReceiver"

        // Active listeners for UI notifications
        var onSmsReceived: ((sender: String, body: String, isOtp: Boolean, isSpam: Boolean) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val fullBody = StringBuilder()
            var sender = "Unknown"
            var timestamp = System.currentTimeMillis()

            for (sms in messages) {
                sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
                fullBody.append(sms.displayMessageBody ?: sms.messageBody ?: "")
                timestamp = sms.timestampMillis
            }

            val body = fullBody.toString()
            Log.d(TAG, "Incoming SMS from $sender: $body")

            // Parse for OTP
            val isOtp = body.contains("OTP", ignoreCase = true) ||
                    body.contains("one time password", ignoreCase = true) ||
                    body.contains("verification code", ignoreCase = true) ||
                    Regex("""\b\d{4,6}\b""").containsMatchIn(body)

            // Parse for Spam
            val isSpam = body.contains("lottery", ignoreCase = true) ||
                    body.contains("won", ignoreCase = true) ||
                    body.contains("claim prize", ignoreCase = true) ||
                    body.contains("free loan", ignoreCase = true) ||
                    body.contains("credit card approved", ignoreCase = true)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AssistantDatabase.getDatabase(context)
                    val status = when {
                        isSpam -> "SPAM_SMS"
                        isOtp -> "OTP_RECEIVED"
                        else -> "RECEIVED"
                    }
                    val logItem = CommunicationLogItem(
                        platform = "SMS",
                        contactName = sender,
                        contactNumber = sender,
                        direction = "INCOMING",
                        status = status,
                        messageContent = body,
                        timestamp = timestamp,
                        durationSeconds = 0
                    )
                    db.assistantDao().insertCommunicationLog(logItem)

                    onSmsReceived?.invoke(sender, body, isOtp, isSpam)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist received SMS: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
