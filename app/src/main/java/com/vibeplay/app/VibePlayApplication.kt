package com.vibeplay.app

import android.app.Application
import com.vibeplay.app.audio.VibeAudioEngine

class VibePlayApplication : Application() {

    lateinit var audioEngine: VibeAudioEngine
        private set

    override fun onCreate() {
        super.onCreate()
        audioEngine = VibeAudioEngine(this)
    }
}
