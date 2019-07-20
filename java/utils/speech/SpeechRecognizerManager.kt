package com.utils.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.lang.IllegalArgumentException

/*
 * Simple wrapper class for the SpeechRecognizer to nicely wrap-up setup and cleanup
 */
class SpeechRecognizerManager (val context: Context, val listener: RecognitionListener) {
  val TAG: String = SpeechRecognizerManager::class.java.simpleName

  private var recognizer: SpeechRecognizer
  private var listenIntent: Intent
  private var isListening: Boolean = false
  private var destroyed: Boolean = false

  init {
    // Set up the recognizer
    recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    recognizer.setRecognitionListener(listener)

    // Build intent to listen to
    listenIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
  }

  fun interruptListening(){
    recognizer.stopListening()

    Log.d(TAG, "Interrupt Listening")
  }

  fun stopListening(){
    recognizer.stopListening()
    recognizer.cancel()
    recognizer.setRecognitionListener(null)
    isListening = false

    Log.d(TAG,"Stop Listening")
  }

  fun startListening() {
    if (isListening || destroyed) return
    isListening = true
    recognizer.setRecognitionListener(listener)

    recognizer.startListening(listenIntent)
    Log.d(TAG,"Start Listening")
  }

  fun destroy(){
    // Unmute audio streams
    stopListening()

    try {
      recognizer.destroy()
    }
    catch(e : IllegalArgumentException){
      // Older versions of Android don't check for null connection properly, catch it
      Log.d(TAG, "Illegal Argument " + e.message)
    }
    destroyed = true

    Log.d(TAG,"Destroyed")
  }
}
