package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AssistantChatMessage
import com.example.service.AppInviteService
import com.example.ui.components.VoiceWaveformBar
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantChatScreen(
    messages: List<AssistantChatMessage>,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    soundLevel: Float,
    preferredLanguage: String,
    voiceOutputAllowed: Boolean = false,
    onToggleVoiceOutput: () -> Unit = {},
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendQuery: (String) -> Unit,
    onSendQueryWithAttachment: (text: String, uri: String?, name: String?, mime: String?, type: String?, size: String?) -> Unit = { t, _, _, _, _, _ -> onSendQuery(t) },
    onSpeakText: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onOpenInviteDialog: () -> Unit = {},
    onTriggerIncomingCall: ((name: String, number: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // File & Photo Attachment States
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedName by remember { mutableStateOf<String?>(null) }
    var attachedMimeType by remember { mutableStateOf<String?>(null) }
    var attachedType by remember { mutableStateOf<String?>(null) }
    var attachedSizeFormatted by remember { mutableStateOf<String?>(null) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            attachedUri = it
            attachedType = "IMAGE"
            attachedMimeType = context.contentResolver.getType(it) ?: "image/*"
            val (name, size) = queryFileMetadata(context, it)
            attachedName = name
            attachedSizeFormatted = size
        }
    }

    // Any File picker (PDF, doc, any file)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore
            }
            attachedUri = it
            val mime = context.contentResolver.getType(it) ?: "application/octet-stream"
            attachedMimeType = mime
            attachedType = if (mime.startsWith("image/")) "IMAGE" else "DOCUMENT"
            val (name, size) = queryFileMetadata(context, it)
            attachedName = name
            attachedSizeFormatted = size
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = if (preferredLanguage == "hi") {
        listOf(
            "कॉल रिसीव टेस्ट करो (Receive Call)",
            "फोटो / फ़ाइल अपलोड करो",
            "दोस्तों को इनवाइट करो (Invite Link)",
            "सहारनपुर की ताज़ा खबरें खोजो",
            "सहारनपुर न्यूज़ फेसबुक पेज पर पब्लिश करो",
            "आज का शेड्यूल क्या है?",
            "माँ को SMS भेजो",
            "राहुल ने क्या कहा था?",
            "ऑफिस पहुँचते ही फाइल लेने का रिमाइंडर सेट करो",
            "कोई नया ईमेल आया क्या?",
            "कॉल स्क्रीन टेस्ट करो"
        )
    } else {
        listOf(
            "Test Receive Call",
            "Upload Photo / File",
            "Invite friends (Direct APK Link)",
            "Search Saharanpur news & post to Facebook",
            "What's on today's schedule?",
            "Send SMS to Mom",
            "Who said what?",
            "Remind me when I reach office to submit bills",
            "Summarize my latest emails",
            "Screen spam calls"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Voice Waveform & Speech indicator bar
        VoiceWaveformBar(
            isListening = isListening,
            isSpeaking = isSpeaking,
            soundLevel = soundLevel,
            statusText = if (isListening) "आप जो बोलेंगे, AI उसे समझेगा और पूरा करेगा..." else "AI उत्तर सुना रहा है..."
        )

        // AI Voice Speaking Allowed Toggle Banner (Controlled by user request)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (voiceOutputAllowed) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clickable { onToggleVoiceOutput() }
                .testTag("toggle_voice_output_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (voiceOutputAllowed) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (voiceOutputAllowed) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (voiceOutputAllowed) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (voiceOutputAllowed) "🔊 AI आवाज़: चालू (बोलकर जवाब देगा)" else "🔇 AI आवाज़: बंद (केवल लिखकर बताएगा)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (voiceOutputAllowed) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (voiceOutputAllowed) "टैप करें म्यूट (केवल टेक्स्ट) करने के लिए" else "टैप करें आवाज़ चालू करने के लिए",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Switch(
                    checked = voiceOutputAllowed,
                    onCheckedChange = { onToggleVoiceOutput() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("voice_output_switch")
                )
            }
        }

        // Chat messages stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyAssistantPlaceholder(preferredLanguage = preferredLanguage)
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = isSpeaking,
                    onSpeakText = { onSpeakText(message.text) },
                    onStopSpeech = onStopSpeech,
                    onOpenInviteDialog = onOpenInviteDialog,
                    onTriggerIncomingCall = {
                        onTriggerIncomingCall?.invoke("Rahul Sharma", "+91 98123 45678")
                    }
                )
            }

            if (isProcessing) {
                item {
                    AiThinkingBubble(preferredLanguage = preferredLanguage)
                }
            }
        }

        // Quick prompts row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                FilterChip(
                    selected = false,
                    onClick = {
                        if (prompt.contains("कॉल रिसीव") || prompt.contains("Receive Call")) {
                            onTriggerIncomingCall?.invoke("Rahul Sharma", "+91 98123 45678")
                        } else if (prompt.contains("फोटो / फ़ाइल") || prompt.contains("Upload")) {
                            showAttachmentDialog = true
                        } else {
                            onSendQuery(prompt)
                        }
                    },
                    label = { Text(prompt, fontSize = 12.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Attachment Preview Bar (if user selected a photo or file)
        AnimatedVisibility(visible = attachedUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (attachedType == "IMAGE") {
                        AsyncImage(
                            model = attachedUri,
                            contentDescription = "Selected Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = "File",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachedName ?: "File",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${attachedType ?: "FILE"} • ${attachedSizeFormatted ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            attachedUri = null
                            attachedName = null
                            attachedMimeType = null
                            attachedType = null
                            attachedSizeFormatted = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input & Voice console bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button (Tap to toggle voice recognition)
                IconButton(
                    onClick = {
                        if (isListening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = if (isListening) {
                                Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                            } else {
                                Brush.linearGradient(listOf(IndigoPrimary, CyanSecondary))
                            },
                            shape = CircleShape
                        )
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Attachment Button (+) for photo/file upload
                IconButton(
                    onClick = { showAttachmentDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("attach_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach File or Photo",
                        tint = if (attachedUri != null) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Text Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (attachedUri != null) "फ़ोटो या फ़ाइल के बारे में पूछें..." else if (preferredLanguage == "hi") "बोलें या सवाल/निर्देश लिखें..." else "Speak or type anything...",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick voice mute/unmute toggle in input box
                            IconButton(
                                onClick = onToggleVoiceOutput,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (voiceOutputAllowed) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Toggle AI Voice",
                                    tint = if (voiceOutputAllowed) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isSpeaking) {
                                IconButton(onClick = onStopSpeech) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Speech",
                                        tint = VioletTertiary
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send button
                val canSend = inputText.isNotBlank() || attachedUri != null
                IconButton(
                    onClick = {
                        if (canSend) {
                            val textToSend = inputText
                            val uri = attachedUri?.toString()
                            val name = attachedName
                            val mime = attachedMimeType
                            val type = attachedType
                            val size = attachedSizeFormatted

                            inputText = ""
                            attachedUri = null
                            attachedName = null
                            attachedMimeType = null
                            attachedType = null
                            attachedSizeFormatted = null

                            onSendQueryWithAttachment(textToSend, uri, name, mime, type, size)
                        }
                    },
                    enabled = canSend && !isProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (canSend) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Attachment Chooser Dialog
    if (showAttachmentDialog) {
        Dialog(onDismissRequest = { showAttachmentDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("attachment_chooser_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (preferredLanguage == "hi") "अपलोड विकल्प चुनें" else "Select Upload Option",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        IconButton(onClick = { showAttachmentDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Upload Photo / Image
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentDialog = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("upload_photo_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(IndigoPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (preferredLanguage == "hi") "गैलरी से फोटो अपलोड करें" else "Upload Photo from Gallery",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (preferredLanguage == "hi") "कोई भी तस्वीर चुनें और AI से पूछें" else "Ask AI about any image or receipt",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: Upload Any File / Document / PDF
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentDialog = false
                                docPickerLauncher.launch(arrayOf("*/*"))
                            }
                            .testTag("upload_document_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(CyanSecondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "File",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (preferredLanguage == "hi") "कोई भी फ़ाइल या PDF अपलोड करें" else "Upload Any File or Document",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (preferredLanguage == "hi") "PDF, टेक्स्ट, वर्ड या अन्य फाइल" else "PDF, text, code, or any documents",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Quick Incoming Call Test
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentDialog = false
                                onTriggerIncomingCall?.invoke("Rahul Sharma", "+91 98123 45678")
                            }
                            .testTag("quick_test_incoming_call_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = "Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (preferredLanguage == "hi") "इनकमिंग कॉल रिसीव टेस्ट करें" else "Test Receive Incoming Call",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                )
                                Text(
                                    text = if (preferredLanguage == "hi") "कॉल आने पर रिसीव या AI स्क्रीन करें" else "Answer or screen test incoming call",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: AssistantChatMessage,
    isSpeaking: Boolean,
    onSpeakText: () -> Unit,
    onStopSpeech: () -> Unit,
    onOpenInviteDialog: () -> Unit = {},
    onTriggerIncomingCall: () -> Unit = {}
) {
    val isUser = message.sender == "USER"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    IndigoPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 2.dp else 1.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Action tag if present
                if (!message.actionType.isNullOrBlank() && message.actionType != "WELCOME") {
                    Surface(
                        color = CyanSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "⚡ ACTION: ${message.actionType}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanSecondary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Attachment Preview in Chat Bubble (if photo or file attached)
                if (!message.attachmentUri.isNullOrBlank()) {
                    if (message.attachmentType == "IMAGE") {
                        AsyncImage(
                            model = message.attachmentUri,
                            contentDescription = "Attached Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${message.attachmentName ?: "Photo"} • ${message.attachmentSizeFormatted ?: ""}",
                                fontSize = 11.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUser) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (isUser) Color.White else IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.attachmentName ?: "Document",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!message.attachmentSizeFormatted.isNullOrBlank()) {
                                        Text(
                                            text = message.attachmentSizeFormatted!!,
                                            fontSize = 10.sp,
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 21.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )

                // If Invite / APK download link is in message, show quick share/download buttons!
                if (!isUser && (message.actionType == "SHARE_INVITE_LINK" || message.text.contains("personal-ai-assistant.apk") || message.text.contains("डाउनलोड") || message.text.contains("इनवाइट"))) {
                    val context = LocalContext.current
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { AppInviteService.shareViaWhatsApp(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_whatsapp_invite_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp इनवाइट", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { AppInviteService.downloadApkDirectly(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_direct_download_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APK डाउनलोड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenInviteDialog,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_open_invite_options_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सभी इनवाइट व शेयरिंग विकल्प", fontSize = 11.sp)
                        }
                    }
                }

                // If Call Receive action is mentioned in message, show button to receive call directly
                if (!isUser && (message.text.contains("कॉल") || message.actionType == "CALL_SCREENING" || message.actionType == "RECEIVE_CALL")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onTriggerIncomingCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_trigger_incoming_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("इनकमिंग कॉल रिसीव टेस्ट करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row: Time + TTS Audio button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onSpeakText,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to response",
                                    tint = VioletTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiThinkingBubble(preferredLanguage: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = IndigoPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (preferredLanguage == "hi") "AI सोच रहा है और प्रोसेस कर रहा है..." else "AI is processing...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyAssistantPlaceholder(preferredLanguage: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(listOf(IndigoPrimary, CyanSecondary)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (preferredLanguage == "hi") "पर्सनल AI असिस्टेंट सक्रिय है" else "Personal AI Assistant Active",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (preferredLanguage == "hi") "कॉल रिसीव, फोटो व फ़ाइल अपलोड, SMS, WhatsApp, ईमेल और शेड्यूल के लिए निर्देश दें।" else "Speak or type to receive calls, upload photos/files, manage SMS, WhatsApp, and schedule.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )
    }
}

/**
 * Extracts display name and human-readable size for a selected content URI.
 */
private fun queryFileMetadata(context: Context, uri: Uri): Pair<String, String> {
    var name = "file"
    var sizeFormatted = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: "file"
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val bytes = cursor.getLong(sizeIndex)
                    sizeFormatted = if (bytes < 1024) {
                        "$bytes B"
                    } else if (bytes < 1024 * 1024) {
                        "${bytes / 1024} KB"
                    } else {
                        String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
                    }
                }
            }
        }
    } catch (e: Exception) {
        name = uri.lastPathSegment ?: "file"
    }
    return Pair(name, sizeFormatted)
}
