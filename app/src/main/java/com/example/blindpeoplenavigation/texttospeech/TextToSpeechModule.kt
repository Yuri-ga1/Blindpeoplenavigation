package com.example.blindpeoplenavigation.texttospeech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechModule(private val context: Context)
    : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val speechQueue: MutableList<String> = mutableListOf()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    fun speakOut(text: String) {
        if (tts?.isSpeaking == true) {
            speechQueue.add(text)
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun speakNext() {
        if (speechQueue.isNotEmpty()) {
            val text = speechQueue.removeAt(0)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                speakNext()
            }

            override fun onError(utteranceId: String?) {
                speakNext()
            }
        })
    }
}