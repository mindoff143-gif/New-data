package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import com.example.viewmodel.AssistantViewModel

@Composable
fun IncomingCallOverlay(
    callState: AssistantViewModel.ActiveCallState,
    onReceiveCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onScreenCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    preferredLanguage: String = "hi"
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { /* Modal call, user must take action */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("incoming_call_overlay_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                val statusText = if (callState.isRinging) {
                    if (preferredLanguage == "hi") "इनकमिंग कॉल आ रही है..." else "Incoming Call..."
                } else {
                    if (preferredLanguage == "hi") "बातचीत चालू है (Call Connected)" else "In Call"
                }

                Surface(
                    color = if (callState.isRinging) Color(0xFF10B981).copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = statusText,
                        color = if (callState.isRinging) Color(0xFF059669) else IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Caller Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = if (callState.isRinging) {
                                Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                            } else {
                                Brush.linearGradient(listOf(IndigoPrimary, CyanSecondary))
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (callState.isRinging) Icons.Default.Call else Icons.Default.PhoneInTalk,
                        contentDescription = "Caller",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Caller Name
                Text(
                    text = callState.callerName.ifBlank { if (preferredLanguage == "hi") "अज्ञात कॉलर" else "Unknown Caller" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                // Phone Number
                Text(
                    text = callState.callerNumber.ifBlank { "+91 98123 45678" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Duration if connected
                if (callState.isConnected) {
                    val minutes = callState.callDurationSeconds / 60
                    val seconds = callState.callDurationSeconds % 60
                    val durationText = String.format("%02d:%02d", minutes, seconds)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons based on Ringing vs Connected
                if (callState.isRinging) {
                    // Option 1: RECEIVE CALL (User answers)
                    Button(
                        onClick = onReceiveCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("receive_call_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Receive Call",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "कॉल उठाएं (Receive Call)" else "Answer Call",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: AI Screen Call
                    OutlinedButton(
                        onClick = onScreenCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_screen_incoming_call_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "AI Screen",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "AI को स्क्रीन करने दें (AI Screening)" else "Screen with AI",
                            fontWeight = FontWeight.SemiBold,
                            color = IndigoPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 3: Decline Call
                    Button(
                        onClick = onDeclineCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("decline_call_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "कॉल अस्वीकार करें (Decline)" else "Decline",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // In-Call Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mute toggle
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (callState.isMuted) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("mute_call_button")
                        ) {
                            Icon(
                                imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (callState.isMuted) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Speaker toggle
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (callState.isSpeakerOn) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("speaker_call_button")
                        ) {
                            Icon(
                                imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speaker",
                                tint = if (callState.isSpeakerOn) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Open in Phone dialer
                        IconButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${callState.callerNumber}")
                                    }
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .background(CyanSecondary.copy(alpha = 0.2f), CircleShape)
                                .testTag("dialer_transfer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = "Open Phone Dialer",
                                tint = CyanSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // End Call button
                    Button(
                        onClick = onEndCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("end_call_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (preferredLanguage == "hi") "कॉल समाप्त करें (End Call)" else "End Call",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
