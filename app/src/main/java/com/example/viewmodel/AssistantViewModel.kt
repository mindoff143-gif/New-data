package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AssistantChatMessage
import com.example.data.model.CalendarEventItem
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.DailyBriefingData
import com.example.data.model.EmailSummaryItem
import com.example.data.model.FacebookPageConfig
import com.example.data.model.ReminderItem
import com.example.data.model.SaharanpurNewsEntity
import com.example.data.repository.AssistantRepository
import com.example.service.CalendarManager
import com.example.service.CallScreenerManager
import com.example.service.CommunicationManager
import com.example.service.FacebookPublisherService
import com.example.service.SpeechManager
import com.example.service.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val communicationManager = CommunicationManager(application)
    private val calendarManager = CalendarManager(application)
    private val callScreenerManager = CallScreenerManager(application)

    private val repository = AssistantRepository(
        dao = database.assistantDao(),
        communicationManager = communicationManager,
        calendarManager = calendarManager,
        callScreenerManager = callScreenerManager,
        context = application
    )

    val ttsManager = TtsManager(application)
    private var speechManager: SpeechManager? = null

    // UI States
    val chatMessages: StateFlow<List<AssistantChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communicationLogs: StateFlow<List<CommunicationLog>> = repository.communicationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderItem>> = repository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactRecord>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emails: StateFlow<List<EmailSummaryItem>> = repository.emails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saharanpurNews: StateFlow<List<SaharanpurNewsEntity>> = repository.saharanpurNews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSearchingNews = MutableStateFlow(false)
    val isSearchingNews: StateFlow<Boolean> = _isSearchingNews.asStateFlow()

    private val _isPublishingToFb = MutableStateFlow(false)
    val isPublishingToFb: StateFlow<Boolean> = _isPublishingToFb.asStateFlow()

    private val _facebookConfig = MutableStateFlow(repository.getFacebookConfig())
    val facebookConfig: StateFlow<FacebookPageConfig> = _facebookConfig.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEventItem>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEventItem>> = _calendarEvents.asStateFlow()

    private val _dailyBriefing = MutableStateFlow<DailyBriefingData?>(null)
    val dailyBriefing: StateFlow<DailyBriefingData?> = _dailyBriefing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _preferredLanguage = MutableStateFlow("hi") // "hi" = Hindi default, "en" = English
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _selectedContact = MutableStateFlow<ContactRecord?>(null)
    val selectedContact: StateFlow<ContactRecord?> = _selectedContact.asStateFlow()

    private val _lastActionNotice = MutableStateFlow<String?>(null)
    val lastActionNotice: StateFlow<String?> = _lastActionNotice.asStateFlow()

    // Voice speaking allowance: By default FALSE so AI speaks ONLY when user enables it!
    private val _voiceOutputAllowed = MutableStateFlow(false)
    val voiceOutputAllowed: StateFlow<Boolean> = _voiceOutputAllowed.asStateFlow()

    // Active Call / Incoming Call state
    data class ActiveCallState(
        val isRinging: Boolean = false,
        val isConnected: Boolean = false,
        val callerName: String = "",
        val callerNumber: String = "",
        val callDurationSeconds: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false
    )

    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()
    private var callTimerJob: kotlinx.coroutines.Job? = null

    // Assistant Name & Wake Word Customization
    private val _assistantName = MutableStateFlow("Jarvis")
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(false)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    // Full-Duplex AI Voice Call Mode (Talking like on a phone call)
    private val _isAiVoiceCallActive = MutableStateFlow(false)
    val isAiVoiceCallActive: StateFlow<Boolean> = _isAiVoiceCallActive.asStateFlow()

    private val _aiCallDuration = MutableStateFlow(0)
    val aiCallDuration: StateFlow<Int> = _aiCallDuration.asStateFlow()

    private val _aiCallStatus = MutableStateFlow("IDLE") // IDLE, LISTENING, PROCESSING, SPEAKING, MUTED
    val aiCallStatus: StateFlow<String> = _aiCallStatus.asStateFlow()

    private val _aiCallUserUtterance = MutableStateFlow("")
    val aiCallUserUtterance: StateFlow<String> = _aiCallUserUtterance.asStateFlow()

    private val _aiCallAiReply = MutableStateFlow("")
    val aiCallAiReply: StateFlow<String> = _aiCallAiReply.asStateFlow()

    private val _aiCallIsMuted = MutableStateFlow(false)
    val aiCallIsMuted: StateFlow<Boolean> = _aiCallIsMuted.asStateFlow()

    private val _aiCallIsSpeaker = MutableStateFlow(true)
    val aiCallIsSpeaker: StateFlow<Boolean> = _aiCallIsSpeaker.asStateFlow()

    private var aiCallTimerJob: kotlinx.coroutines.Job? = null

    init {
        speechManager = SpeechManager(application) { recognizedSpeech ->
            onVoiceInputReceived(recognizedSpeech)
        }

        viewModelScope.launch {
            speechManager?.isListening?.collect { _isListening.value = it }
        }
        viewModelScope.launch {
            speechManager?.soundLevel?.collect { _soundLevel.value = it }
        }

        // When TTS finishes speaking during an AI Voice Call, automatically listen for the user's reply!
        ttsManager.onSpeechCompleted = {
            if (_isAiVoiceCallActive.value && !_aiCallIsMuted.value) {
                _aiCallStatus.value = "LISTENING"
                val langCode = if (_preferredLanguage.value == "hi") "hi-IN" else "en-IN"
                speechManager?.startListening(langCode)
            }
        }

        // Live Call & SMS Broadcast Receivers Hooks
        com.example.receiver.CallBroadcastReceiver.onIncomingCallDetected = { name, number, isSpam ->
            triggerIncomingCall(name, number)
            val spamAlert = if (isSpam) " [स्पैम कॉल का जोखिम]" else ""
            _lastActionNotice.value = "📞 इनकमिंग कॉल: $number$spamAlert"
        }

        com.example.receiver.CallBroadcastReceiver.onCallEnded = {
            endIncomingCall()
        }

        com.example.receiver.SmsBroadcastReceiver.onSmsReceived = { sender, body, isOtp, isSpam ->
            val alert = when {
                isOtp -> "🔐 नया OTP SMS प्राप्त हुआ ($sender)"
                isSpam -> "⚠️ संदिग्ध स्पैम SMS ब्लॉक/दर्ज हुआ ($sender)"
                else -> "✉️ नया SMS प्राप्त हुआ ($sender)"
            }
            _lastActionNotice.value = alert
        }

        refreshCalendarEvents()
        loadDailyBriefing()
    }

    fun setPreferredLanguage(lang: String) {
        _preferredLanguage.value = lang
        ttsManager.setLanguage(lang)
    }

    fun toggleVoiceOutput() {
        _voiceOutputAllowed.value = !_voiceOutputAllowed.value
        if (!_voiceOutputAllowed.value) {
            ttsManager.stop()
        }
        val msg = if (_voiceOutputAllowed.value) "AI आवाज़ चालू (Voice Enabled)" else "AI आवाज़ बंद: केवल टेक्स्ट मोड (Text Only Mode)"
        _lastActionNotice.value = msg
    }

    fun setVoiceOutputAllowed(allowed: Boolean) {
        _voiceOutputAllowed.value = allowed
        if (!allowed) {
            ttsManager.stop()
        }
    }

    fun startListening() {
        ttsManager.stop()
        val langCode = if (_preferredLanguage.value == "hi") "hi-IN" else "en-IN"
        speechManager?.startListening(langCode)
    }

    fun stopListening() {
        speechManager?.stopListening()
    }

    fun setAssistantName(name: String) {
        val trimmed = name.trim().ifBlank { "Jarvis" }
        _assistantName.value = trimmed
        _lastActionNotice.value = "असिस्टेंट का नाम बदला: $trimmed"
    }

    fun toggleWakeWord(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        speechManager?.isWakeWordStandby = enabled
        if (enabled) {
            val langCode = if (_preferredLanguage.value == "hi") "hi-IN" else "en-IN"
            speechManager?.startListening(langCode)
            _lastActionNotice.value = "⚡ वेकअप डिटेक्शन सक्रिय: \"${_assistantName.value} wake up\" बोलें"
        } else {
            speechManager?.stopListening()
            _lastActionNotice.value = "वेकअप डिटेक्शन बंद"
        }
    }

    fun startAiVoiceCall() {
        if (_isAiVoiceCallActive.value) return
        _isAiVoiceCallActive.value = true
        _aiCallDuration.value = 0
        _aiCallUserUtterance.value = ""
        _aiCallAiReply.value = ""
        _aiCallIsMuted.value = false
        _aiCallStatus.value = "SPEAKING"

        aiCallTimerJob?.cancel()
        aiCallTimerJob = viewModelScope.launch {
            while (_isAiVoiceCallActive.value) {
                delay(1000)
                _aiCallDuration.value += 1
            }
        }

        val greeting = if (_preferredLanguage.value == "hi") {
            "नमस्ते! मैं ${_assistantName.value} हूँ। फोन कॉल की तरह आप जो भी पूछना चाहते हैं, बोलिए।"
        } else {
            "Hello! I am ${_assistantName.value}. Feel free to speak, I am on the line with you."
        }
        _aiCallAiReply.value = greeting
        ttsManager.speak(greeting, _preferredLanguage.value)
    }

    fun endAiVoiceCall() {
        _isAiVoiceCallActive.value = false
        aiCallTimerJob?.cancel()
        ttsManager.stop()
        speechManager?.stopListening()
        _aiCallStatus.value = "IDLE"
        _lastActionNotice.value = "AI वॉयस कॉल समाप्त (बातचीत: ${_aiCallDuration.value}s)"

        viewModelScope.launch {
            repository.recordReceivedCall(
                contactName = "${_assistantName.value} (AI)",
                contactNumber = "AI-VOICE-CALL",
                durationSeconds = _aiCallDuration.value
            )
        }
    }

    fun toggleAiCallMute() {
        _aiCallIsMuted.value = !_aiCallIsMuted.value
        if (_aiCallIsMuted.value) {
            speechManager?.stopListening()
            _aiCallStatus.value = "MUTED"
        } else {
            _aiCallStatus.value = "LISTENING"
            val langCode = if (_preferredLanguage.value == "hi") "hi-IN" else "en-IN"
            speechManager?.startListening(langCode)
        }
    }

    fun toggleAiCallSpeaker() {
        _aiCallIsSpeaker.value = !_aiCallIsSpeaker.value
    }

    private fun onVoiceInputReceived(spokenText: String) {
        if (spokenText.isBlank()) return

        // 1. If in active AI Voice Call mode:
        if (_isAiVoiceCallActive.value) {
            _aiCallUserUtterance.value = spokenText
            _aiCallStatus.value = "PROCESSING"
            viewModelScope.launch {
                try {
                    val result = repository.processUserCommand(spokenText, _preferredLanguage.value)
                    _aiCallAiReply.value = result.responseText
                    _aiCallStatus.value = "SPEAKING"
                    ttsManager.speak(result.responseText, _preferredLanguage.value)
                    // Note: when TTS finishes, ttsManager.onSpeechCompleted resumes listening automatically!
                } catch (e: Exception) {
                    _aiCallStatus.value = "IDLE"
                    _aiCallAiReply.value = "त्रुटि: ${e.message}"
                }
            }
            return
        }

        // 2. Check for Wake Word trigger
        val lower = spokenText.lowercase().trim()
        val nameLower = _assistantName.value.lowercase().trim()
        val isWakeWord = lower.contains("$nameLower wake up") ||
                lower.contains("$nameLower wakeup") ||
                lower.contains("$nameLower जागो") ||
                lower.contains("wake up $nameLower") ||
                lower.contains("wakeup $nameLower") ||
                lower.contains("सुनो $nameLower") ||
                (lower.contains(nameLower) && (lower.contains("wake") || lower.contains("जागो") || lower.contains("सुनो")))

        if (isWakeWord) {
            _lastActionNotice.value = "⚡ ${_assistantName.value} वेकअप सक्रिय हुआ!"
            startAiVoiceCall()
            return
        }

        // 3. Regular chat input
        sendUserQuery(spokenText)
    }

    fun sendUserQuery(prompt: String) {
        if (prompt.isBlank() || _isProcessing.value) return
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = repository.processUserCommand(prompt, _preferredLanguage.value)
                _lastActionNotice.value = if (result.actionType != "GENERAL_AI") "Action: ${result.actionType}" else null
                // Speak only when voice output is allowed by the user!
                if (_voiceOutputAllowed.value) {
                    ttsManager.speak(result.responseText, _preferredLanguage.value)
                }
            } catch (e: Exception) {
                _lastActionNotice.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun sendUserQueryWithAttachment(
        prompt: String,
        attachmentUri: String?,
        attachmentName: String?,
        attachmentMimeType: String?,
        attachmentType: String?,
        attachmentSizeFormatted: String?
    ) {
        if (_isProcessing.value) return
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = repository.processUserCommandWithAttachment(
                    userInput = prompt,
                    attachmentUri = attachmentUri,
                    attachmentName = attachmentName,
                    attachmentMimeType = attachmentMimeType,
                    attachmentType = attachmentType,
                    attachmentSizeFormatted = attachmentSizeFormatted,
                    preferredLanguage = _preferredLanguage.value
                )
                _lastActionNotice.value = if (result.actionType != "GENERAL_AI") "Action: ${result.actionType}" else null
                // Speak only when voice output is allowed by the user!
                if (_voiceOutputAllowed.value) {
                    ttsManager.speak(result.responseText, _preferredLanguage.value)
                }
            } catch (e: Exception) {
                _lastActionNotice.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Call Receiving & Active In-Call Management
    fun triggerIncomingCall(callerName: String, callerNumber: String) {
        _activeCall.value = ActiveCallState(
            isRinging = true,
            isConnected = false,
            callerName = callerName,
            callerNumber = callerNumber
        )
    }

    fun receiveIncomingCall() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isRinging = false, isConnected = true, callDurationSeconds = 0)
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    it.copy(callDurationSeconds = it.callDurationSeconds + 1)
                }
            }
        }
        val msg = if (_preferredLanguage.value == "hi") "${current.callerName} से कॉल कनेक्ट हो गई है।" else "Call connected with ${current.callerName}"
        _lastActionNotice.value = msg
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun endActiveCall() {
        val current = _activeCall.value
        callTimerJob?.cancel()
        callTimerJob = null
        if (current != null && current.isConnected) {
            val duration = maxOf(1, current.callDurationSeconds)
            viewModelScope.launch {
                repository.recordReceivedCall(
                    contactName = current.callerName,
                    contactNumber = current.callerNumber,
                    durationSeconds = duration
                )
            }
            _lastActionNotice.value = "कॉल समाप्त हुई (बातचीत: ${duration} सेकंड)"
        }
        _activeCall.value = null
    }

    fun declineIncomingCall() {
        val current = _activeCall.value
        _activeCall.value = null
        if (current != null) {
            _lastActionNotice.value = "कॉल अस्वीकार की गई (${current.callerName})"
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text, _preferredLanguage.value)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun refreshCalendarEvents() {
        _calendarEvents.value = repository.getCalendarEvents()
    }

    fun loadDailyBriefing() {
        viewModelScope.launch {
            _dailyBriefing.value = repository.generateDailyBriefing()
        }
    }

    fun playDailyBriefingAloud() {
        val briefing = _dailyBriefing.value ?: return
        val speech = if (_preferredLanguage.value == "hi") briefing.summaryTextHindi else briefing.summaryTextEnglish
        ttsManager.speak(speech, _preferredLanguage.value)
    }

    fun toggleReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.toggleReminder(reminder)
        }
    }

    fun addCustomReminder(title: String, timeInMillis: Long, isLocation: Boolean, location: String?) {
        viewModelScope.launch {
            val reminder = ReminderItem(
                title = title,
                timeInMillis = timeInMillis,
                isLocationBased = isLocation,
                locationName = location,
                isCompleted = false,
                isSyncedToCalendar = true
            )
            repository.addReminder(reminder)
            val msg = if (_preferredLanguage.value == "hi") "रिमाइंडर सुरक्षित कर लिया गया है" else "Reminder saved"
            _lastActionNotice.value = msg
            ttsManager.speak(msg, _preferredLanguage.value)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun simulateCallScreening(phoneNumber: String, name: String? = null) {
        viewModelScope.launch {
            val decision = repository.simulateIncomingCall(phoneNumber, name)
            _lastActionNotice.value = "कॉल स्क्रीन: ${decision.callerName} (${decision.actionTaken})"
            ttsManager.speak(decision.aiGreetingSpeech, _preferredLanguage.value)
        }
    }

    fun sendManualMessage(platform: String, contactName: String, number: String, text: String) {
        viewModelScope.launch {
            repository.sendManualMessage(platform, contactName, number, text)
            val notice = if (_preferredLanguage.value == "hi") "$contactName को $platform भेजा गया" else "$platform sent to $contactName"
            _lastActionNotice.value = notice
            ttsManager.speak(notice, _preferredLanguage.value)
        }
    }

    fun selectContact(contact: ContactRecord?) {
        _selectedContact.value = contact
    }

    fun readEmailSummary(email: EmailSummaryItem) {
        viewModelScope.launch {
            database.assistantDao().markEmailAsRead(email.id)
            val summary = if (_preferredLanguage.value == "hi") email.aiSummaryHindi else email.aiSummaryEnglish
            ttsManager.speak("${email.sender} से ईमेल समरी: $summary", _preferredLanguage.value)
        }
    }

    fun clearActionNotice() {
        _lastActionNotice.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Saharanpur News & Facebook Publishing
    fun searchSaharanpurNews(query: String = "") {
        if (_isSearchingNews.value) return
        _isSearchingNews.value = true
        viewModelScope.launch {
            try {
                val results = repository.searchAndFetchSaharanpurNews(query)
                val msg = if (_preferredLanguage.value == "hi") {
                    "सहारनपुर की ${results.size} ताज़ा ख़बरें प्राप्त हुईं।"
                } else {
                    "Fetched ${results.size} latest news stories for Saharanpur."
                }
                _lastActionNotice.value = msg
            } catch (e: Exception) {
                _lastActionNotice.value = "न्यूज़ खोजने में त्रुटि: ${e.message}"
            } finally {
                _isSearchingNews.value = false
            }
        }
    }

    fun publishNewsToFacebook(article: SaharanpurNewsEntity, customText: String? = null) {
        if (_isPublishingToFb.value) return
        _isPublishingToFb.value = true
        viewModelScope.launch {
            try {
                val result = repository.publishNewsArticleToFacebook(article.id, customText)
                _lastActionNotice.value = result.message
                if (result.isSuccess) {
                    ttsManager.speak(
                        if (_preferredLanguage.value == "hi") "सहारनपुर समाचार आपके फेसबुक पेज पर पब्लिश कर दिया गया है!" else "Saharanpur news published to your Facebook page!",
                        _preferredLanguage.value
                    )
                }
            } catch (e: Exception) {
                _lastActionNotice.value = "फेसबुक पब्लिशिंग त्रुटि: ${e.message}"
            } finally {
                _isPublishingToFb.value = false
            }
        }
    }

    fun shareNewsDirectlyToFacebook(context: android.content.Context, article: SaharanpurNewsEntity, customText: String? = null) {
        viewModelScope.launch {
            try {
                val postContent = customText ?: article.formattedFbPost
                val result = FacebookPublisherService.shareViaFacebookAppDirect(
                    context = context,
                    postText = postContent,
                    imageUrl = article.imageUrl,
                    linkUrl = article.originalUrl,
                    articleId = article.id
                )
                if (result.isSuccess) {
                    repository.updateNewsArticle(
                        article.copy(
                            isPublishedToFb = true,
                            publishedTimestamp = System.currentTimeMillis(),
                            fbPostId = "DIRECT_FB_APP"
                        )
                    )
                }
                _lastActionNotice.value = result.message
            } catch (e: Exception) {
                _lastActionNotice.value = "फेसबुक खोलने में त्रुटि: ${e.message}"
            }
        }
    }

    fun shareNewsViaIntent(context: android.content.Context, article: SaharanpurNewsEntity) {
        shareNewsDirectlyToFacebook(context, article)
    }

    fun saveFacebookConfig(config: FacebookPageConfig) {
        repository.saveFacebookConfig(config)
        _facebookConfig.value = config
        _lastActionNotice.value = "फेसबुक पेज सेटिंग्स सुरक्षित की गईं"
    }

    fun updateNewsArticle(article: SaharanpurNewsEntity) {
        viewModelScope.launch {
            repository.updateNewsArticle(article)
        }
    }

    fun unpublishNewsArticle(article: SaharanpurNewsEntity) {
        viewModelScope.launch {
            repository.updateNewsArticle(
                article.copy(
                    isPublishedToFb = false,
                    fbPostId = null,
                    publishedTimestamp = null
                )
            )
            _lastActionNotice.value = "पोस्ट को ताज़ा समाचार फ़ीड में वापस लाया गया"
        }
    }

    fun deleteNewsArticle(id: Long) {
        viewModelScope.launch {
            repository.deleteNewsArticle(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager.release()
    }
}
