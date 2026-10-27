package com.audioshelf.app.sound

data class SoundUi(
    val scope: EqScope = EqScope.Global,
    val trackAvailable: Boolean = false,
    val trackTitle: String = "",
    val eq: Eq = Eq.Flat,
    val ownEq: Boolean = false,
    val fx: Fx = Fx.Off,
    val ownFx: Boolean = false,
)

interface SoundActions {
    fun onScope(scope: EqScope)
    fun onBand(index: Int, db: Int)
    fun onPreset(id: String)
    fun onReset()
    fun onUseGlobal()
    fun onSkipSilence(on: Boolean)
    fun onFxUseGlobal()

    companion object {
        val None = object : SoundActions {
            override fun onScope(scope: EqScope) {}
            override fun onBand(index: Int, db: Int) {}
            override fun onPreset(id: String) {}
            override fun onReset() {}
            override fun onUseGlobal() {}
            override fun onSkipSilence(on: Boolean) {}
            override fun onFxUseGlobal() {}
        }
    }
}
