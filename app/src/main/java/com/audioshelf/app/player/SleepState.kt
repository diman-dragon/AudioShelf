package com.audioshelf.app.player

private const val SLEEP_GRACE_MS = 1500L

/** What the sleep button shows. It follows the engine's timer, so an expired timer turns the button off. */
class SleepState {
    var choice: SleepChoice = SleepChoice.Off
        private set
    private var armedAt = 0L

    fun sync(s: PlayState) {
        val armed = System.currentTimeMillis() - armedAt > SLEEP_GRACE_MS
        if (choice != SleepChoice.Off && armed && !(s.sleepLeftMs > 0 || s.sleepAtChapterEnd)) choice = SleepChoice.Off
    }

    fun cycle(engine: PlaybackEngine) {
        choice = choice.next()
        armedAt = System.currentTimeMillis()
        engine.setSleep(choice.minutes.toLong(), choice == SleepChoice.Chapter)
    }
}
