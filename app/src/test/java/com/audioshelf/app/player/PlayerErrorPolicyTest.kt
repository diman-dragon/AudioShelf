package com.audioshelf.app.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerErrorPolicyTest {
    private val policy = PlayerErrorPolicy()

    @Test fun codesAreSortedIntoKinds() {
        assertEquals(FailureKind.FileMissing, failureKindOf(2005))
        assertEquals(FailureKind.NoAccess, failureKindOf(2006))
        assertEquals(FailureKind.Other, failureKindOf(2000))
        assertEquals(FailureKind.Corrupt, failureKindOf(3001))
        assertEquals(FailureKind.Decoder, failureKindOf(4001))
        assertEquals(FailureKind.Corrupt, failureKindOf(4005))
        assertEquals(FailureKind.AudioOutput, failureKindOf(5001))
        assertEquals(FailureKind.Other, failureKindOf(1000))
    }

    @Test fun aMissingFileIsSkippedNotRetried() {
        assertEquals(Recovery.SkipFile, policy.decide(FailureKind.FileMissing, 0, hasNextFile = true))
        assertEquals(Recovery.GiveUp, policy.decide(FailureKind.FileMissing, 0, hasNextFile = false))
    }

    @Test fun audioOutputGetsOneRetryAndSkippingCannotHelp() {
        assertEquals(Recovery.Retry, policy.decide(FailureKind.AudioOutput, 0, hasNextFile = true))
        assertEquals(Recovery.GiveUp, policy.decide(FailureKind.AudioOutput, 1, hasNextFile = true))
    }

    @Test fun decoderRetriesOnceThenSkipsThenStops() {
        assertEquals(Recovery.Retry, policy.decide(FailureKind.Decoder, 0, hasNextFile = true))
        assertEquals(Recovery.SkipFile, policy.decide(FailureKind.Decoder, 1, hasNextFile = true))
        assertEquals(Recovery.GiveUp, policy.decide(FailureKind.Decoder, 1, hasNextFile = false))
    }

    @Test fun retryingAlwaysEnds() {
        var attempt = 0
        while (policy.decide(FailureKind.Other, attempt, hasNextFile = false) == Recovery.Retry) {
            attempt++
            check(attempt < 10) { "retries never stop" }
        }
        assertEquals(1, attempt)
    }
}
