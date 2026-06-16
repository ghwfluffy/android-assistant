package com.ghwfluffy.assistantwrapper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.concurrent.Executors

class VoiceActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var statusText: TextView
    private lateinit var conversationScroll: ScrollView
    private lateinit var conversationLog: LinearLayout
    private lateinit var stopButton: Button
    private lateinit var recordButton: Button
    private lateinit var cancelButton: Button
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var warmSession = false
    private val chatTurns = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        warmConnection()
        startRecordingWhenAllowed()
    }

    override fun onDestroy() {
        recorder?.release()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            setStatus("Microphone permission is required.")
            updateButtons(recording = false, uploading = false)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }
        statusText = TextView(this).apply {
            textSize = 18f
            text = "Preparing..."
        }
        conversationLog = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }
        conversationScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(conversationLog)
        }
        stopButton = Button(this).apply {
            text = "Stop recording"
            setOnClickListener { stopAndSendRecording() }
        }
        recordButton = Button(this).apply {
            text = "Record again"
            setOnClickListener {
                warmConnection()
                startRecordingWhenAllowed()
            }
        }
        cancelButton = Button(this).apply {
            text = "Cancel"
            setOnClickListener { cancelRecording() }
        }
        root.addView(statusText)
        root.addView(stopButton)
        root.addView(recordButton)
        root.addView(cancelButton)
        root.addView(TextView(this).apply {
            text = "Conversation"
            textSize = 13f
            gravity = Gravity.START
        })
        root.addView(conversationScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(root)
        updateButtons(recording = false, uploading = false)
    }

    private fun warmConnection() {
        executor.execute {
            val authenticated = MobileHttpClient.warmAgentSession()
            mainHandler.post {
                warmSession = authenticated
                if (!authenticated && recorder == null) {
                    setStatus("Sign in is required before sending voice.")
                }
            }
        }
    }

    private fun startRecordingWhenAllowed() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
            return
        }
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST)
    }

    private fun startRecording() {
        cancelRecording(deleteOnly = true)
        val output = File.createTempFile("voice-", ".m4a", cacheDir)
        recordingFile = output
        recorder = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        setStatus("Recording...")
        updateButtons(recording = true, uploading = false)
    }

    private fun stopAndSendRecording() {
        val file = recordingFile ?: return
        runCatching {
            recorder?.stop()
        }
        recorder?.release()
        recorder = null
        recordingFile = null
        setStatus("Sending...")
        updateButtons(recording = false, uploading = true)
        executor.execute {
            val result = runCatching {
                if (!warmSession) {
                    warmSession = MobileHttpClient.warmAgentSession()
                }
                if (!warmSession) {
                    throw IllegalStateException("Sign in is required before sending voice.")
                }
                MobileHttpClient.sendVoicePrompt(file, recentContext())
            }
            file.delete()
            mainHandler.post {
                result.onSuccess { prompt ->
                    val reply = prompt.displayText()
                    addConversationTurn("You", prompt.transcript)
                    addConversationTurn("Agent", reply)
                    setStatus("Ready")
                    updateButtons(recording = false, uploading = false)
                }.onFailure { error ->
                    val message = error.message ?: "Voice request failed."
                    addConversationTurn("System", message, includeInContext = false)
                    setStatus(message)
                    updateButtons(recording = false, uploading = false)
                    if (!warmSession || message.contains("HTTP 401")) {
                        warmSession = false
                        openSignIn()
                    }
                }
            }
        }
    }

    private fun cancelRecording(deleteOnly: Boolean = false) {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        recordingFile?.delete()
        recordingFile = null
        if (!deleteOnly) {
            setStatus("Cancelled")
            updateButtons(recording = false, uploading = false)
        }
    }

    private fun openSignIn() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MobileConfig.EXTRA_START_PATH, MobileConfig.agentBasePath + "/api/v1/auth/login")
        }
        startActivity(intent)
    }

    private fun recentContext(): String {
        return chatTurns.takeLast(8).joinToString("\n")
    }

    private fun addConversationTurn(speaker: String, body: CharSequence, includeInContext: Boolean = true) {
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 10)
        }
        val label = TextView(this).apply {
            text = speaker
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(31, 41, 55))
        }
        val message = TextView(this).apply {
            text = body
            textSize = 16f
            setTextColor(Color.rgb(17, 24, 39))
            setTextIsSelectable(true)
        }
        group.addView(label)
        group.addView(message)
        conversationLog.addView(group, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        if (includeInContext) {
            chatTurns += "$speaker: $body"
        }
        scrollConversationToBottom()
    }

    private fun scrollConversationToBottom() {
        conversationScroll.post {
            conversationScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun updateButtons(recording: Boolean, uploading: Boolean) {
        stopButton.isEnabled = recording && !uploading
        stopButton.visibility = if (recording) View.VISIBLE else View.GONE
        recordButton.isEnabled = !recording && !uploading
        cancelButton.isEnabled = recording || uploading
    }

    private fun setStatus(value: String) {
        statusText.text = value
    }

    private fun newRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
    }

    companion object {
        private const val RECORD_AUDIO_REQUEST = 3001
    }
}
