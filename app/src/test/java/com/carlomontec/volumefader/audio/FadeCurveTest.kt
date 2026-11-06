package com.carlomontec.volumefader.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FadeCurveTest {

    @Test
    fun `test all curves start at full volume factor and end at zero`() {
        FadeCurve.entries.forEach { curve ->
            assertEquals(1.0f, curve.calculateVolumeFactor(0.0f), 0.001f)
            assertEquals(0.0f, curve.calculateVolumeFactor(1.0f), 0.001f)
        }
    }

    @Test
    fun `test monotonicity across progress range`() {
        FadeCurve.entries.forEach { curve ->
            var lastFactor = 1.0f
            for (step in 1..100) {
                val progress = step / 100.0f
                val factor = curve.calculateVolumeFactor(progress)
                assertTrue(
                    "Curve ${curve.name} failed monotonicity at progress $progress: $factor > $lastFactor",
                    factor <= lastFactor
                )
                lastFactor = factor
            }
        }
    }

    @Test
    fun `test logarithmic curve maintains higher volume at midpoint for loudness compensation`() {
        val linearMid = FadeCurve.LINEAR.calculateVolumeFactor(0.5f)
        val logMid = FadeCurve.LOGARITHMIC.calculateVolumeFactor(0.5f)

        // Logarithmic factor at midpoint should stay higher to compensate for human hearing
        assertTrue(logMid > linearMid)
    }
}
