package com.audioshelf.app.player

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRulesTest {
    @Test
    fun aNotificationNeverStopsThePlayback() {
        for (change in listOf(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)) {
            assertEquals(FocusAction.Ignore, FocusRules.onChange(change, inCall = false, pausedByFocus = false))
        }
    }

    @Test
    fun aCallPausesAndTheEndOfTheCallResumes() {
        assertEquals(
            FocusAction.PauseAndResume,
            FocusRules.onChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, inCall = true, pausedByFocus = false),
        )
        assertEquals(FocusAction.Resume, FocusRules.onChange(AudioManager.AUDIOFOCUS_GAIN, inCall = false, pausedByFocus = true))
    }

    @Test
    fun gainWithoutOurPauseChangesNothing() {
        assertEquals(FocusAction.Ignore, FocusRules.onChange(AudioManager.AUDIOFOCUS_GAIN, inCall = false, pausedByFocus = false))
    }

    @Test
    fun anotherAppTakingTheAudioForGoodPausesForGood() {
        assertEquals(FocusAction.PauseForGood, FocusRules.onChange(AudioManager.AUDIOFOCUS_LOSS, inCall = false, pausedByFocus = false))
    }

    @Test
    fun onlyCallModesCountAsACall() {
        assertTrue(FocusRules.isCall(AudioManager.MODE_RINGTONE))
        assertTrue(FocusRules.isCall(AudioManager.MODE_IN_CALL))
        assertTrue(FocusRules.isCall(AudioManager.MODE_IN_COMMUNICATION))
        assertFalse(FocusRules.isCall(AudioManager.MODE_NORMAL))
    }
}
