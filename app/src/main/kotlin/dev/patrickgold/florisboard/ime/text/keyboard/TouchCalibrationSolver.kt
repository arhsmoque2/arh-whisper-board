/*
 * Copyright (C) 2026 ARH / FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.text.keyboard

import dev.patrickgold.florisboard.ime.nlp.latin.TouchScoring
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Single empirical tap record associating a target key with the user's actual landing coordinate.
 * Coordinates are normalized to key-width units.
 */
data class TapSample(
    val targetCode: Int,
    val targetX: Float,
    val targetY: Float,
    val tapX: Float,
    val tapY: Float,
) {
    val dx: Float get() = tapX - targetX
    val dy: Float get() = tapY - targetY
    val distanceSq: Float get() = dx * dx + dy * dy
    val distance: Float get() = sqrt(distanceSq)
}

/**
 * Proposed calibration solution computed from empirical tap evidence.
 */
data class CalibrationProposal(
    val profile: TouchCalibrationProfile,
    val recommendations: List<String>,
    val averageDriftMagnitude: Float,
    val initialTypoRate: Float,
    val estimatedImprovementPercent: Float,
)

/**
 * Statistical solver calculating macro window ergonomics and per-key centroid displacements
 * from ground-truth touch samples.
 */
object TouchCalibrationSolver {

    // Keycodes for thumb reach boundary analysis (QWERTY layout)
    private val LEFT_COLUMN_CODES = setOf('q'.code, 'a'.code, 'z'.code, 'Q'.code, 'A'.code, 'Z'.code)
    private val RIGHT_COLUMN_CODES = setOf('p'.code, 'm'.code, 'P'.code, 'M'.code)
    private val BOTTOM_ROW_CODES = setOf('z'.code, 'x'.code, 'c'.code, 'v'.code, 'b'.code, 'n'.code, 'm'.code)

    /**
     * Solve for optimal profile given a series of tap samples and current baseline profile.
     */
    fun solve(
        samples: List<TapSample>,
        baseProfile: TouchCalibrationProfile = TouchCalibrationProfile.Default,
        orientation: String = TouchCalibrationProfile.ORIENTATION_PORTRAIT,
    ): CalibrationProposal {
        if (samples.isEmpty()) {
            return CalibrationProposal(
                profile = baseProfile,
                recommendations = listOf("No tap samples collected. Baseline profile maintained."),
                averageDriftMagnitude = 0f,
                initialTypoRate = 0f,
                estimatedImprovementPercent = 0f,
            )
        }

        val recommendations = mutableListOf<String>()

        // 1. Group samples by keycode
        val keyGroups = samples.groupBy { it.targetCode }

        // 2. Compute per-key mean drift vector and variance
        val keyOffsets = mutableMapOf<Int, KeyCalibrationOffset>()
        var totalDriftSum = 0f
        var totalDistSqSum = 0f
        var typoCount = 0

        for ((code, group) in keyGroups) {
            val count = group.size.toFloat()
            val meanDx = group.sumOf { it.dx.toDouble() }.toFloat() / count
            val meanDy = group.sumOf { it.dy.toDouble() }.toFloat() / count

            for (sample in group) {
                totalDriftSum += sample.distance
                totalDistSqSum += sample.distanceSq
                // Any tap landing further than 0.5 key-widths is an intended hit landing in neighbor
                if (sample.distance > 0.45f) {
                    typoCount++
                }
            }

            // If drift is statistically significant (> 0.03 key widths), capture offset
            if (abs(meanDx) >= 0.03f || abs(meanDy) >= 0.03f) {
                // Key offset shifts the effective detection center towards the user's landing center
                keyOffsets[code] = KeyCalibrationOffset(
                    code = code,
                    dxNormalized = meanDx.coerceIn(-0.25f, 0.25f),
                    dyNormalized = meanDy.coerceIn(-0.25f, 0.25f),
                )
            }
        }

        val avgDrift = totalDriftSum / samples.size
        val avgDistSq = totalDistSqSum / samples.size
        val initialTypoRate = typoCount.toFloat() / samples.size

        // 3. Macro window padding analysis
        // Left edge retraction: positive dx on left column
        val leftSamples = samples.filter { it.targetCode in LEFT_COLUMN_CODES }
        val avgLeftDx = if (leftSamples.isNotEmpty()) leftSamples.sumOf { it.dx.toDouble() }.toFloat() / leftSamples.size else 0f

        // Right edge retraction: negative dx on right column
        val rightSamples = samples.filter { it.targetCode in RIGHT_COLUMN_CODES }
        val avgRightDx = if (rightSamples.isNotEmpty()) rightSamples.sumOf { it.dx.toDouble() }.toFloat() / rightSamples.size else 0f

        // Bottom row retraction: negative dy on bottom row (taps landing high)
        val bottomSamples = samples.filter { it.targetCode in BOTTOM_ROW_CODES }
        val avgBottomDy = if (bottomSamples.isNotEmpty()) bottomSamples.sumOf { it.dy.toDouble() }.toFloat() / bottomSamples.size else 0f

        var newPaddingLeft = baseProfile.paddingLeftDp
        var newPaddingRight = baseProfile.paddingRightDp
        var newPaddingBottom = baseProfile.paddingBottomDp

        if (avgLeftDx > 0.06f) {
            val suggestedPad = (avgLeftDx * 40f).coerceIn(2f, 12f).roundToInt().toFloat()
            newPaddingLeft = max(newPaddingLeft, suggestedPad)
            recommendations.add("Increased left bezel margin by ${newPaddingLeft.toInt()}dp to bring Q/A/Z into comfortable thumb reach.")
        }

        if (avgRightDx < -0.06f) {
            val suggestedPad = (abs(avgRightDx) * 40f).coerceIn(2f, 12f).roundToInt().toFloat()
            newPaddingRight = max(newPaddingRight, suggestedPad)
            recommendations.add("Increased right bezel margin by ${newPaddingRight.toInt()}dp to prevent P/M edge undershoot.")
        }

        if (avgBottomDy < -0.06f) {
            val suggestedPad = (abs(avgBottomDy) * 40f).coerceIn(2f, 10f).roundToInt().toFloat()
            newPaddingBottom = max(newPaddingBottom, suggestedPad)
            recommendations.add("Elevated bottom margin by ${newPaddingBottom.toInt()}dp to prevent thumb hyperextension on spacebar and bottom row.")
        }

        // 4. Calibrated touch variance (sigma2)
        // Scaled against empirical tap variance
        val calibratedSigma2 = (avgDistSq * 1.2).coerceIn(0.16, 0.32)

        if (keyOffsets.isNotEmpty()) {
            recommendations.add("Calibrated centroid offsets for ${keyOffsets.size} high-frequency keys.")
        }

        val improvementEst = (initialTypoRate * 0.75f * 100f).coerceAtLeast(15f).coerceAtMost(85f)

        val solvedProfile = baseProfile.copy(
            id = "calibrated_${System.currentTimeMillis() % 10000}",
            name = "Calibrated ($orientation)",
            orientation = orientation,
            isEnabled = true,
            paddingLeftDp = newPaddingLeft,
            paddingRightDp = newPaddingRight,
            paddingBottomDp = newPaddingBottom,
            sigma2 = calibratedSigma2,
            keyOffsets = keyOffsets,
        )

        return CalibrationProposal(
            profile = solvedProfile,
            recommendations = recommendations,
            averageDriftMagnitude = avgDrift,
            initialTypoRate = initialTypoRate,
            estimatedImprovementPercent = improvementEst,
        )
    }
}
