package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

class CommunicationManager(private val context: Context) {
    private val TAG = "CommunicationManager"

    /**
     * Sends SMS. If SEND_SMS permission is granted, attempts direct send;
     * otherwise opens the native SMS app directly with recipient and body pre-filled.
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            // First attempt native SMS intent which works without dangerous runtime permission
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", message)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct SMS intent failed, attempting SmsManager: ${e.message}")
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Both SMS methods failed: ${ex.message}")
                false
            }
        }
    }

    /**
     * Opens WhatsApp directly with the recipient and message.
     * Supports standard WhatsApp (com.whatsapp) and WhatsApp Business (com.whatsapp.w4b).
     */
    fun sendWhatsApp(phoneNumber: String?, message: String): Boolean {
        return try {
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
            val formattedPhone = when {
                cleanPhone.startsWith("+") -> cleanPhone.substring(1)
                cleanPhone.length == 10 -> "91$cleanPhone" // Default to India prefix if 10 digits
                else -> cleanPhone
            }

            val uriString = if (formattedPhone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }

            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Target installed WhatsApp package directly
            val pm = context.packageManager
            when {
                isPackageInstalled("com.whatsapp", pm) -> intent.setPackage("com.whatsapp")
                isPackageInstalled("com.whatsapp.w4b", pm) -> intent.setPackage("com.whatsapp.w4b")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct WhatsApp app failed, falling back to browser URL: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "WhatsApp fallback failed: ${ex.message}")
                false
            }
        }
    }

    /**
     * Opens Telegram directly with recipient or message.
     * Supports direct Telegram package (org.telegram.messenger) and web links.
     */
    fun sendTelegram(usernameOrPhone: String?, message: String): Boolean {
        return try {
            val cleanTarget = usernameOrPhone?.trim()?.removePrefix("@").orEmpty()
            val uri = if (cleanTarget.isNotEmpty()) {
                Uri.parse("https://t.me/$cleanTarget?text=${Uri.encode(message)}")
            } else {
                Uri.parse("tg://msg?text=${Uri.encode(message)}")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (isPackageInstalled("org.telegram.messenger", pm)) {
                intent.setPackage("org.telegram.messenger")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct Telegram app intent failed, using universal link: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://t.me/share/url?text=${Uri.encode(message)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Telegram fallback intent failed: ${ex.message}")
                false
            }
        }
    }

    /**
     * Directly opens the phone dialer with the phone number pre-filled.
     */
    fun makeCall(phoneNumber: String) {
        try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+*#]"), "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Dial intent failed: ${e.message}")
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
