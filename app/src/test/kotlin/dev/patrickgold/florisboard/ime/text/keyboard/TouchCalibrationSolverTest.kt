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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TouchCalibrationSolverTest {

    @Test
    fun `solver handles empty tap samples gracefully`() {
        val proposal = TouchCalibrationSolver.solve(emptyList())
        assertEquals(0f, proposal.averageDriftMagnitude)
        assertEquals(0f, proposal.initialTypoRate)
        assertTrue(proposal.recommendations.isNotEmpty())
    }

    @Test
    fun `systematic thumb retraction on A and Q produces offset and increases left padding`() {
        // Simulate user aiming for 'A' (x=0.5f) but landing at x=0.65f (+0.15f drift towards S)
        val samples = listOf(
            TapSample(targetCode = 'a'.code, targetX = 0.5f, targetY = 1.5f, tapX = 0.65f, tapY = 1.5f),
            TapSample(targetCode = 'a'.code, targetX = 0.5f, targetY = 1.5f, tapX = 0.63f, tapY = 1.51f),
            TapSample(targetCode = 'a'.code, targetX = 0.5f, targetY = 1.5f, tapX = 0.67f, tapY = 1.49f),
            // User aiming for 'Q' (x=0.5f) but landing at x=0.62f (+0.12f drift towards W)
            TapSample(targetCode = 'q'.code, targetX = 0.5f, targetY = 0.5f, tapX = 0.62f, tapY = 0.5f),
            TapSample(targetCode = 'q'.code, targetX = 0.5f, targetY = 0.5f, tapX = 0.64f, tapY = 0.52f),
        )

        val proposal = TouchCalibrationSolver.solve(samples)

        // Verify left padding increased
        assertTrue(proposal.profile.paddingLeftDp > 0f)

        // Verify key offset captured for 'a' and 'q'
        val offsetA = proposal.profile.keyOffsets['a'.code]
        assertTrue(offsetA != null && offsetA.dxNormalized > 0.10f)

        val offsetQ = proposal.profile.keyOffsets['q'.code]
        assertTrue(offsetQ != null && offsetQ.dxNormalized > 0.10f)

        // Verify recommendations mention left bezel
        assertTrue(proposal.recommendations.any { it.contains("left bezel margin") })
    }

    @Test
    fun `systematic right thumb undershoot on P produces negative offset and increases right padding`() {
        val samples = listOf(
            TapSample(targetCode = 'p'.code, targetX = 9.5f, targetY = 0.5f, tapX = 9.36f, tapY = 0.5f),
            TapSample(targetCode = 'p'.code, targetX = 9.5f, targetY = 0.5f, tapX = 9.34f, tapY = 0.51f),
            TapSample(targetCode = 'p'.code, targetX = 9.5f, targetY = 0.5f, tapX = 9.38f, tapY = 0.49f),
        )

        val proposal = TouchCalibrationSolver.solve(samples)

        // Verify right padding increased
        assertTrue(proposal.profile.paddingRightDp > 0f)

        // Verify key offset captured for 'p' is negative (shifting centroid towards left)
        val offsetP = proposal.profile.keyOffsets['p'.code]
        assertTrue(offsetP != null && offsetP.dxNormalized < -0.10f)

        // Verify recommendations mention right bezel
        assertTrue(proposal.recommendations.any { it.contains("right bezel margin") })
    }
}
