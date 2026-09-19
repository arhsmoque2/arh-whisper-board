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

    @Test
    fun `touch on left edge of S resolves to adjacent A character key`() {
        val aKey = TextKey(data = TextKeyData(code = 'a'.code, type = KeyType.CHARACTER, label = "a"))
        aKey.touchBounds.apply {
            left = 0f; top = 100f; right = 100f; bottom = 200f
        }
        val sKey = TextKey(data = TextKeyData(code = 's'.code, type = KeyType.CHARACTER, label = "s"))
        sKey.touchBounds.apply {
            left = 100f; top = 100f; right = 200f; bottom = 200f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(aKey, sKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch at x = 115f (15% into 's'): resolves to 'a' due to thumb retraction protection
        val retractedTouch = keyboard.getKeyForPos(115f, 150f)
        assertEquals('a'.code, (retractedTouch?.data as? TextKeyData)?.code)

        // Touch at x = 150f (50% into 's'): resolves normally to 's'
        val normalTouch = keyboard.getKeyForPos(150f, 150f)
        assertEquals('s'.code, (normalTouch?.data as? TextKeyData)?.code)
    }

    @Test
    fun `touch on left edge of W resolves to adjacent Q character key`() {
        val qKey = TextKey(data = TextKeyData(code = 'q'.code, type = KeyType.CHARACTER, label = "q"))
        qKey.touchBounds.apply {
            left = 0f; top = 0f; right = 100f; bottom = 100f
        }
        val wKey = TextKey(data = TextKeyData(code = 'w'.code, type = KeyType.CHARACTER, label = "w"))
        wKey.touchBounds.apply {
            left = 100f; top = 0f; right = 200f; bottom = 100f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(qKey, wKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch at x = 110f (10% into 'w'): resolves to 'q'
        val retractedTouch = keyboard.getKeyForPos(110f, 50f)
        assertEquals('q'.code, (retractedTouch?.data as? TextKeyData)?.code)

        // Touch at x = 160f (60% into 'w'): resolves to 'w'
        val normalTouch = keyboard.getKeyForPos(160f, 50f)
        assertEquals('w'.code, (normalTouch?.data as? TextKeyData)?.code)
    }

    @Test
    fun `touch on left edge of X resolves to adjacent Z character key`() {
        val zKey = TextKey(data = TextKeyData(code = 'z'.code, type = KeyType.CHARACTER, label = "z"))
        zKey.touchBounds.apply {
            left = 0f; top = 200f; right = 100f; bottom = 300f
        }
        val xKey = TextKey(data = TextKeyData(code = 'x'.code, type = KeyType.CHARACTER, label = "x"))
        xKey.touchBounds.apply {
            left = 100f; top = 200f; right = 200f; bottom = 300f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(zKey, xKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch at x = 112f (12% into 'x'): resolves to 'z'
        val retractedTouch = keyboard.getKeyForPos(112f, 250f)
        assertEquals('z'.code, (retractedTouch?.data as? TextKeyData)?.code)
    }

    @Test
    fun `touch on right edge of O resolves to adjacent P character key`() {
        val oKey = TextKey(data = TextKeyData(code = 'o'.code, type = KeyType.CHARACTER, label = "o"))
        oKey.touchBounds.apply {
            left = 800f; top = 0f; right = 900f; bottom = 100f
        }
        val pKey = TextKey(data = TextKeyData(code = 'p'.code, type = KeyType.CHARACTER, label = "p"))
        pKey.touchBounds.apply {
            left = 900f; top = 0f; right = 1000f; bottom = 100f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(oKey, pKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch at x = 885f (85% into 'o'): resolves to 'p'
        val extensionTouch = keyboard.getKeyForPos(885f, 50f)
        assertEquals('p'.code, (extensionTouch?.data as? TextKeyData)?.code)

        // Touch at x = 850f (50% into 'o'): resolves to 'o'
        val normalTouch = keyboard.getKeyForPos(850f, 50f)
        assertEquals('o'.code, (normalTouch?.data as? TextKeyData)?.code)
    }

    @Test
    fun `touch on left edge of comma resolves to adjacent M character key`() {
        val mKey = TextKey(data = TextKeyData(code = 'm'.code, type = KeyType.CHARACTER, label = "m"))
        mKey.touchBounds.apply {
            left = 600f; top = 200f; right = 700f; bottom = 300f
        }
        val commaKey = TextKey(data = TextKeyData(code = ','.code, type = KeyType.CHARACTER, label = ","))
        commaKey.touchBounds.apply {
            left = 700f; top = 200f; right = 800f; bottom = 300f
        }

        val keyboard = TextKeyboard(
            arrangement = arrayOf(arrayOf(mKey, commaKey)),
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null
        )

        // Touch at x = 712f (12% into comma): resolves to 'm'
        val cornerTouch = keyboard.getKeyForPos(712f, 250f)
        assertEquals('m'.code, (cornerTouch?.data as? TextKeyData)?.code)
    }
}
