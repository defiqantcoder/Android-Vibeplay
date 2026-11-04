package com.vibeplay.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerAccessTest {
    @Test
    fun `app and trusted media controllers can connect`() {
        assertTrue(isPlaybackControllerAllowed(controllerUid = 1234, appUid = 1234, isTrusted = false))
        assertTrue(isPlaybackControllerAllowed(controllerUid = 5678, appUid = 1234, isTrusted = true))
    }

    @Test
    fun `untrusted and unknown controllers cannot connect`() {
        assertFalse(isPlaybackControllerAllowed(controllerUid = 5678, appUid = 1234, isTrusted = false))
        assertFalse(isPlaybackControllerAllowed(controllerUid = -1, appUid = 1234, isTrusted = false))
        assertFalse(isPlaybackControllerAllowed(controllerUid = -1, appUid = -1, isTrusted = false))
    }
}
