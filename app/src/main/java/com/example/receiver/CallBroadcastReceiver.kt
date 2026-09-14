package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.service.CallScreenerManager

class CallBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallBroadcastReceiver"

        // Active listeners for runtime overlay / ViewModel
        var onIncomingCallDetected: ((callerName: String?, callerNumber: String, isSpam: Boolean) -> Unit)? = null
        var onCallEnded: (() -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

            Log.d(TAG, "Phone State: $state, Incoming: $incomingNumber")

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val screener = CallScreenerManager(context)
                    val result = screener.screenIncomingCall(incomingNumber, null)
                    val displayName = if (result.isSpam) {
                        "⚠️ ${result.callerName} (Spam Risk)"
                    } else {
                        result.callerName
                    }

                    onIncomingCallDetected?.invoke(displayName, incomingNumber, result.isSpam)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    onCallEnded?.invoke()
                }
            }
        }
    }
}
