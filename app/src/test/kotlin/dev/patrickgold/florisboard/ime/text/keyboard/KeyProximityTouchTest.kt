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

import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyProximityTouchTest {

    @Test
    fun `touch on left edge of backspace resolves to adjacent M character key`() {
        val mKey = TextKey(data = TextKeyData(code = 'm'.code, type = KeyType.CHARACTER, label = "m"))
        mKey.touchBounds.apply {
            left = 500f
            top = 200f
            right = 600f
            bottom = 300f
        }

        val backspaceKey = TextKey(data = TextKeyData(code = KeyCode.DELETE, type = KeyType.ENTER_EDITING, label = "del"))
        backspaceKey.touchBounds.apply {
            left = 600f
            top = 200f
            right = 800f
            bottom = 300f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(mKey, backspaceKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // 1. Touch well inside Backspace (x = 700f): resolves to Backspace
        val normalDelete = keyboard.getKeyForPos(700f, 250f)
        assertEquals(KeyCode.DELETE, (normalDelete?.data as? TextKeyData)?.code)

        // 2. Touch on the left 25% edge of Backspace (x = 610f): resolves to 'm'
        val guardedTouch = keyboard.getKeyForPos(610f, 250f)
        assertEquals('m'.code, (guardedTouch?.data as? TextKeyData)?.code)
    }

    @Test
    fun `touch on left edge of enter resolves to adjacent character key`() {
        val nKey = TextKey(data = TextKeyData(code = 'n'.code, type = KeyType.CHARACTER, label = "n"))
        nKey.touchBounds.apply {
            left = 400f
            top = 200f
            right = 500f
            bottom = 300f
        }

        val enterKey = TextKey(data = TextKeyData(code = KeyCode.ENTER, type = KeyType.ENTER_EDITING, label = "enter"))
        enterKey.touchBounds.apply {
            left = 500f
            top = 200f
            right = 700f
            bottom = 300f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(nKey, enterKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch on left edge (x = 515f, inside first 25% of 200px enterKey): resolves to 'n'
        val guardedTouch = keyboard.getKeyForPos(515f, 250f)
        assertEquals('n'.code, (guardedTouch?.data as? TextKeyData)?.code)
    }
}
