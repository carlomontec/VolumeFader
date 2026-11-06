package com.carlomontec.volumefader.model

import com.carlomontec.volumefader.audio.FadeCurve

data class FadeConfig(
    val durationMinutes: Int = 30,
    val curve: FadeCurve = FadeCurve.LOGARITHMIC,
    val pauseMediaOnFinish: Boolean = true
)

sealed interface FadeStatus {
    data object Idle : FadeStatus
    data class Running(
        val totalDurationMs: Long,
        val remainingMs: Long,
        val currentVolumeStep: Int,
        val maxVolumeStep: Int,
        val targetVolumePercent: Float
    ) : FadeStatus
    data object Completed : FadeStatus
}
