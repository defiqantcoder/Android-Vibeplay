package com.vibeplay.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class WearMainActivity : ComponentActivity() {
    private val syncManager by lazy { WearSyncManager.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val playbackState by syncManager.playbackState.collectAsState()
            val tracks by syncManager.tracks.collectAsState()
            PixelWatchApp(playbackState = playbackState, tracks = tracks, syncManager = syncManager)
        }
    }

    override fun onStart() {
        super.onStart()
        syncManager.register()
    }

    override fun onStop() {
        syncManager.unregister()
        super.onStop()
    }
}
