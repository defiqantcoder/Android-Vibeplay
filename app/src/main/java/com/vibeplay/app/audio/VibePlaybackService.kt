package com.vibeplay.app.audio

import android.app.PendingIntent
import android.content.Intent
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vibeplay.app.MainActivity
import com.vibeplay.app.VibePlayApplication

class VibePlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as? VibePlayApplication
        val player = app?.audioEngine?.exoPlayer

        if (player != null) {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(sessionActivityPendingIntent)
                .setCallback(PlaybackSessionCallback())
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

internal fun isPlaybackControllerAllowed(controllerUid: Int, appUid: Int, isTrusted: Boolean): Boolean =
    (controllerUid >= 0 && controllerUid == appUid) || isTrusted

@OptIn(UnstableApi::class)
internal class PlaybackSessionCallback : MediaSession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        return if (isPlaybackControllerAllowed(controller.uid, Process.myUid(), controller.isTrusted)) {
            super.onConnect(session, controller)
        } else {
            MediaSession.ConnectionResult.reject()
        }
    }
}
