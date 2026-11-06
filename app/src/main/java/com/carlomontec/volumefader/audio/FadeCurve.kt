package com.carlomontec.volumefader.audio

import kotlin.math.ln

enum class FadeCurve(val displayName: String, val description: String) {
    LOGARITHMIC(
        "Perceptual (dB)",
        "Compensates for human loudness perception. Smooth and gradual at quiet levels."
    ),
    LINEAR(
        "Linear",
        "Constant rate of decrease across the entire duration."
    ),
    EXPONENTIAL(
        "Exponential",
        "Steeper initial drop with a very quiet, lingering tail."
    );

    /**
     * Calculates the normalized volume factor [0.0f..1.0f] given normalized progress [0.0f..1.0f] (0 = start, 1 = end).
     */
    fun calculateVolumeFactor(progress: Float): Float {
        val clampedProgress = progress.coerceIn(0.0f, 1.0f)
        val remaining = 1.0f - clampedProgress
        return when (this) {
            LINEAR -> remaining
            LOGARITHMIC -> {
                // Natural perceptual curve: ln(1 + 9 * remaining) / ln(10)
                // At remaining = 1.0 -> ln(10)/ln(10) = 1.0
                // At remaining = 0.0 -> ln(1)/ln(10) = 0.0
                (ln(1.0 + 9.0 * remaining) / ln(10.0)).toFloat()
            }
            EXPONENTIAL -> {
                remaining * remaining
            }
        }
    }
}
