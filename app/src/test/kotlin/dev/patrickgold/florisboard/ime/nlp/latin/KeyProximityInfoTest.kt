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

package dev.patrickgold.florisboard.ime.nlp.latin

import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.keyboard.KeyCalibrationOffset
import dev.patrickgold.florisboard.ime.text.keyboard.TextKey
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.text.keyboard.TouchCalibrationProfile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyProximityInfoTest {

    @BeforeTest
    @AfterTest
    fun resetState() {
        KeyProximityInfo.activeProfile = null
        TouchScoring.configuredSigma2 = TouchScoring.TOUCH_SIGMA2
    }

    private fun createKey(code: Int, left: Float, right: Float, top: Float = 0f, bottom: Float = 100f): TextKey {
        val key = TextKey(data = TextKeyData(code = code, type = KeyType.CHARACTER, label = code.toChar().toString()))
        key.visibleBounds.apply {
            this.left = left
            this.right = right
            this.top = top
            this.bottom = bottom
        }
        return key
    }

    @Test
    fun `profile offsets shift key centroids in KeyProximityInfo Layout`() {
        val aKey = createKey('a'.code, 0f, 100f) // center x = 50f
        val sKey = createKey('s'.code, 100f, 200f) // center x = 150f

        val profile = TouchCalibrationProfile(
            isEnabled = true,
            sigma2 = 0.25,
            keyOffsets = mapOf(
                'a'.code to KeyCalibrationOffset('a'.code, dxNormalized = 0.12f, dyNormalized = 0f),
            )
        )

        KeyProximityInfo.update(listOf(aKey, sKey), profile)
        val layout = KeyProximityInfo.snapshot()
        assertNotNull(layout)

        // Baseline keyWidth = 100f
        // Expected 'a' normalized x: 50f/100f + 0.12f = 0.62f
        // Squared distance from (0.62f, 0.50f) to key 'a' (index 0) should be 0.0f
        val distToA = layout.sqDistance(0, 0.62f, 0.50f)
        assertTrue(distToA < 0.0001f, "Expected calibrated centroid for 'a' to be at x=0.62f")

        // 's' has no offset, so center remains 150f/100f = 1.50f
        val distToS = layout.sqDistance(1, 1.50f, 0.50f)
        assertTrue(distToS < 0.0001f, "Expected uncalibrated centroid for 's' to be at x=1.50f")

        // Verify TouchScoring.configuredSigma2 was synced
        assertEquals(0.25, TouchScoring.configuredSigma2)
    }

    @Test
    fun `subtype isolation ensures QWERTY calibration does not bleed into Arabic or Numeric keys`() {
        // Arabic Alef (0x0627) and Beh (0x0628)
        val alefKey = createKey(0x0627, 0f, 100f)
        val behKey = createKey(0x0628, 100f, 200f)

        // QWERTY calibration profile
        val qwertyProfile = TouchCalibrationProfile(
            isEnabled = true,
            keyOffsets = mapOf(
                'a'.code to KeyCalibrationOffset('a'.code, dxNormalized = 0.12f, dyNormalized = 0f),
                's'.code to KeyCalibrationOffset('s'.code, dxNormalized = 0.05f, dyNormalized = 0f),
            )
        )

        KeyProximityInfo.update(listOf(alefKey, behKey), qwertyProfile)
        val layout = KeyProximityInfo.snapshot()
        assertNotNull(layout)

        // Neither alef nor beh match 'a' or 's', so their centroids should have 0 shift (center x=0.50f, 1.50f)
        val distToAlef = layout.sqDistance(0, 0.50f, 0.50f)
        assertTrue(distToAlef < 0.0001f, "Arabic Alef must not receive QWERTY offset")

        val distToBeh = layout.sqDistance(1, 1.50f, 0.50f)
        assertTrue(distToBeh < 0.0001f, "Arabic Beh must not receive QWERTY offset")
    }

    @Test
    fun `disabled profile applies zero offsets`() {
        val aKey = createKey('a'.code, 0f, 100f)
        val profile = TouchCalibrationProfile(
            isEnabled = false,
            keyOffsets = mapOf(
                'a'.code to KeyCalibrationOffset('a'.code, dxNormalized = 0.12f, dyNormalized = 0f),
            )
        )

        KeyProximityInfo.update(listOf(aKey), profile)
        val layout = KeyProximityInfo.snapshot()
        assertNotNull(layout)

        val distToA = layout.sqDistance(0, 0.50f, 0.50f)
        assertTrue(distToA < 0.0001f, "Disabled profile must not shift key centroid")
    }
}
