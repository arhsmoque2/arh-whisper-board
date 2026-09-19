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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TouchCalibrationProfileTest {

    @Test
    fun `preset for poco f7 portrait has correct ergonomic offsets and margins`() {
        val preset = TouchCalibrationProfile.PocoF7PortraitPreset
        assertEquals("poco_f7_portrait", preset.id)
        assertEquals(TouchCalibrationProfile.ORIENTATION_PORTRAIT, preset.orientation)
        assertTrue(preset.isEnabled)
        assertEquals(4f, preset.paddingLeftDp)
        assertEquals(4f, preset.paddingRightDp)
        assertEquals(6f, preset.paddingBottomDp)
        assertEquals(0.22, preset.sigma2)

        // Verify left retraction offsets for A, Q, Z
        val (dxA, dyA) = preset.getOffset('a'.code)
        assertEquals(0.12f, dxA)
        assertEquals(0f, dyA)

        val (dxQ, _) = preset.getOffset('q'.code)
        assertEquals(0.10f, dxQ)

        val (dxZ, _) = preset.getOffset('z'.code)
        assertEquals(0.10f, dxZ)

        // Verify right retraction offsets for P, M
        val (dxP, _) = preset.getOffset('p'.code)
        assertEquals(-0.10f, dxP)

        val (dxM, _) = preset.getOffset('m'.code)
        assertEquals(-0.08f, dxM)

        // Keys not in profile return 0f, 0f
        val (dxG, dyG) = preset.getOffset('g'.code)
        assertEquals(0f, dxG)
        assertEquals(0f, dyG)
    }

    @Test
    fun `serialization and deserialization roundtrip preserves profile fidelity`() {
        val original = TouchCalibrationProfile.PocoF7PortraitPreset
        val serialized = TouchCalibrationProfile.Serializer.serialize(original)
        assertTrue(serialized.contains("poco_f7_portrait"))
        assertTrue(serialized.contains("paddingLeftDp"))

        val deserialized = TouchCalibrationProfile.Serializer.deserialize(serialized)
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.paddingLeftDp, deserialized.paddingLeftDp)
        assertEquals(original.paddingRightDp, deserialized.paddingRightDp)
        assertEquals(original.keyOffsets.size, deserialized.keyOffsets.size)
        assertEquals(original.getOffset('a'.code), deserialized.getOffset('a'.code))
    }

    @Test
    fun `corrupt json falls back to default profile safely without exception`() {
        val corrupt = "{ this is not valid json }"
        val result = TouchCalibrationProfile.Serializer.deserialize(corrupt)
        assertEquals(TouchCalibrationProfile.Default.id, result.id)
    }

    @Test
    fun `checkpoint stack manages undoable calibration iterations`() {
        val emptyStack = CalibrationCheckpointStack()
        assertFalse(emptyStack.canRollback)
        assertNull(emptyStack.current)

        val p1 = TouchCalibrationProfile(id = "iter1", name = "Iteration 1")
        val cp1 = CalibrationCheckpoint(iteration = 1, profile = p1, measuredTypoRate = 0.20f)
        val stack1 = emptyStack.push(cp1)

        assertEquals(1, stack1.checkpoints.size)
        assertFalse(stack1.canRollback) // Only 1 checkpoint, cannot rollback beyond initial
        assertEquals("iter1", stack1.current?.profile?.id)

        val p2 = TouchCalibrationProfile(id = "iter2", name = "Iteration 2")
        val cp2 = CalibrationCheckpoint(iteration = 2, profile = p2, measuredTypoRate = 0.08f)
        val stack2 = stack1.push(cp2)

        assertEquals(2, stack2.checkpoints.size)
        assertTrue(stack2.canRollback)
        assertEquals("iter2", stack2.current?.profile?.id)

        // Rollback to iteration 1
        val (restored, stackAfterRollback) = stack2.rollback()
        assertNotNull(restored)
        assertEquals("iter1", restored.profile.id)
        assertEquals(1, stackAfterRollback.checkpoints.size)
        assertFalse(stackAfterRollback.canRollback)
    }
}
