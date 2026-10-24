package com.audioshelf.app.sound

data class SoundUi(
    val scope: EqScope = EqScope.Global,
    val trackAvailable: Boolean = false,
    val trackTitle: String = "",
    val eq: Eq = Eq.Flat,
    val ownEq: Boolean = false,
    val boost: Int = 100,
    val normOn: Boolean = true,
    val norm: NormState = NormState(),
    val pending: Int = 0,
    val totalFiles: Int = 0,
)

interface SoundActions {
    fun onScope(scope: EqScope)
    fun onBand(index: Int, db: Int)
    fun onPreset(id: String)
    fun onReset()
    fun onUseGlobal()
    fun onBoost(percent: Int)
    fun onNormOn(on: Boolean)
    fun onNormStart()
    fun onNormStop()

    companion object {
        val None = object : SoundActions {
            override fun onScope(scope: EqScope) {}
            override fun onBand(index: Int, db: Int) {}
            override fun onPreset(id: String) {}
            override fun onReset() {}
            override fun onUseGlobal() {}
            override fun onBoost(percent: Int) {}
            override fun onNormOn(on: Boolean) {}
            override fun onNormStart() {}
            override fun onNormStop() {}
        }
    }
}
