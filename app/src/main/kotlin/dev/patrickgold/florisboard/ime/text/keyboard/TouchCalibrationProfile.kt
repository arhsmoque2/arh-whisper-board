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
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Normalized per-key displacement offset calculated from empirical tap evidence.
 * Expressed in key-width units (resolution- and DPI-independent).
 */
@Serializable
data class KeyCalibrationOffset(
    val code: Int,
    val dxNormalized: Float = 0f,
    val dyNormalized: Float = 0f,
)

/**
 * Persistent calibration profile storing macro ergonomics (window padding)
 * and micro ergonomics (per-key centroid displacements & touch variance).
 */
@Serializable
data class TouchCalibrationProfile(
    val id: String = DEFAULT_PROFILE_ID,
    val name: String = "Default",
    val orientation: String = ORIENTATION_PORTRAIT,
    val isEnabled: Boolean = true,
    val paddingLeftDp: Float = 0f,
    val paddingRightDp: Float = 0f,
    val paddingBottomDp: Float = 0f,
    val sigma2: Double = TouchScoring.TOUCH_SIGMA2,
    val keyOffsets: Map<Int, KeyCalibrationOffset> = emptyMap(),
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "default"
        const val ORIENTATION_PORTRAIT = "PORTRAIT"
        const val ORIENTATION_LANDSCAPE = "LANDSCAPE"

        val Default = TouchCalibrationProfile()

        /**
         * Preset profile for Poco F7 portrait mode addressing thumb reach ergonomics.
         * Narrows keyboard width slightly via horizontal padding, lifts bottom boundary,
         * and pre-calibrates thumb retraction vectors for left column (Q, A, Z) and right column (P, M).
         */
        val PocoF7PortraitPreset = TouchCalibrationProfile(
            id = "poco_f7_portrait",
            name = "Poco F7 Portrait (6.83\" 1280x2772 120Hz)",
            orientation = ORIENTATION_PORTRAIT,
            isEnabled = true,
            paddingLeftDp = 4f,
            paddingRightDp = 4f,
            paddingBottomDp = 6f,
            sigma2 = 0.22,
            keyOffsets = mapOf(
                'a'.code to KeyCalibrationOffset('a'.code, dxNormalized = 0.12f, dyNormalized = 0f),
                'q'.code to KeyCalibrationOffset('q'.code, dxNormalized = 0.10f, dyNormalized = 0f),
                'z'.code to KeyCalibrationOffset('z'.code, dxNormalized = 0.10f, dyNormalized = 0f),
                'p'.code to KeyCalibrationOffset('p'.code, dxNormalized = -0.10f, dyNormalized = 0f),
                'm'.code to KeyCalibrationOffset('m'.code, dxNormalized = -0.08f, dyNormalized = 0f),
            )
        )
        val PocoF7Portrait = PocoF7PortraitPreset

        /**
         * Preset profile for Poco F7 landscape mode (6.83" 2772x1280, 853 dp width).
         * Adds lateral inset padding (64 dp left/right) and bottom margin (8 dp)
         * to condense the wide 853 dp canvas into a comfortable two-thumb reach zone,
         * with inward centroid shifts on center keys (G, H, B, V, T, Y) to eliminate stretch strain.
         */
        val PocoF7LandscapePreset = TouchCalibrationProfile(
            id = "poco_f7_landscape",
            name = "Poco F7 Landscape (6.83\" 2772x1280 120Hz)",
            orientation = ORIENTATION_LANDSCAPE,
            isEnabled = true,
            paddingLeftDp = 64f,
            paddingRightDp = 64f,
            paddingBottomDp = 8f,
            sigma2 = 0.20,
            keyOffsets = mapOf(
                'g'.code to KeyCalibrationOffset('g'.code, dxNormalized = -0.10f, dyNormalized = 0f),
                'h'.code to KeyCalibrationOffset('h'.code, dxNormalized = 0.10f, dyNormalized = 0f),
                'b'.code to KeyCalibrationOffset('b'.code, dxNormalized = -0.08f, dyNormalized = 0f),
                'v'.code to KeyCalibrationOffset('v'.code, dxNormalized = -0.06f, dyNormalized = 0f),
                't'.code to KeyCalibrationOffset('t'.code, dxNormalized = -0.08f, dyNormalized = 0f),
                'y'.code to KeyCalibrationOffset('y'.code, dxNormalized = 0.08f, dyNormalized = 0f),
            )
        )
        val PocoF7Landscape = PocoF7LandscapePreset
    }

    /**
     * Look up normalized (dx, dy) offset for a given character code.
     * Returns 0f, 0f if no calibration offset is registered for this key.
     */
    fun getOffset(code: Int): Pair<Float, Float> {
        val entry = keyOffsets[code] ?: return 0f to 0f
        return entry.dxNormalized to entry.dyNormalized
    }

    /**
     * Checkpoint serialization helper for JetPref.
     */
    object Serializer : PreferenceSerializer<TouchCalibrationProfile> {
        override fun serialize(value: TouchCalibrationProfile): String = Json.encodeToString(value)
        override fun deserialize(value: String): TouchCalibrationProfile {
            return try {
                Json.decodeFromString(value)
            } catch (e: Throwable) {
                flogError { "Failed to deserialize TouchCalibrationProfile: ${e.message}" }
                Default
            }
        }
    }
}

/**
 * An immutable calibration snapshot capturing a single iteration during interactive calibration.
 */
@Serializable
data class CalibrationCheckpoint(
    val timestamp: Long = System.currentTimeMillis(),
    val iteration: Int = 0,
    val profile: TouchCalibrationProfile,
    val measuredTypoRate: Float = 0f,
    val description: String = "",
)

/**
 * Revertible stack of calibration checkpoints allowing the user to undo iterations.
 */
@Serializable
data class CalibrationCheckpointStack(
    val checkpoints: List<CalibrationCheckpoint> = emptyList(),
) {
    val canRollback: Boolean get() = checkpoints.size > 1

    val current: CalibrationCheckpoint? get() = checkpoints.lastOrNull()

    fun push(checkpoint: CalibrationCheckpoint): CalibrationCheckpointStack {
        return copy(checkpoints = checkpoints + checkpoint)
    }

    fun rollback(): Pair<CalibrationCheckpoint?, CalibrationCheckpointStack> {
        if (!canRollback) return null to this
        val newStack = checkpoints.dropLast(1)
        val restored = newStack.last()
        return restored to copy(checkpoints = newStack)
    }

    object Serializer : PreferenceSerializer<CalibrationCheckpointStack> {
        override fun serialize(value: CalibrationCheckpointStack): String = Json.encodeToString(value)
        override fun deserialize(value: String): CalibrationCheckpointStack {
            return try {
                Json.decodeFromString(value)
            } catch (e: Throwable) {
                flogError { "Failed to deserialize CalibrationCheckpointStack: ${e.message}" }
                CalibrationCheckpointStack()
            }
        }
    }
}
