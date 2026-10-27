package com.audioshelf.app

/**
 * Features that are NOT built and are reserved for a future paid ("premium") tier.
 * Nothing here is wired to the UI. Billing is not built, so everything shipped today is free.
 *
 * Planned model (decided, not implemented): 14 days of full access from first launch, then a one-time
 * purchase; voluntary donations stay available at any time. Gate every check through [Premium.isAvailable]
 * so the trial / purchase logic can be added in one place.
 *
 * - [AutomationIntents]: Tasker / MacroDroid broadcast intents (play, pause, rewind, sleep timer, ...).
 * - [AndroidAuto]: Android Auto browsing and playback (MediaLibraryService, automotive_app_desc).
 */
enum class PremiumFeature {
    AutomationIntents,
    AndroidAuto,
}

object Premium {
    val planned: List<PremiumFeature> = PremiumFeature.entries

    fun isAvailable(@Suppress("UNUSED_PARAMETER") feature: PremiumFeature): Boolean = false
}
