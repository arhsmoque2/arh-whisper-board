/*
 * Copyright (C) 2026 ARH Whisper Board Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.ime.smartbar.quick

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class QuickSnippetsManagerTest {

    @Test
    fun `parseSnippets filters empty and whitespace lines`() {
        val raw = "  /resume  \n\n  /usage\n   \n/clear\n"
        val parsed = QuickSnippetsManager.parseSnippets(raw)
        assertEquals(listOf("/resume", "/usage", "/clear"), parsed)
    }

    @Test
    fun `serializeSnippets joins trimmed lines with newline`() {
        val snippets = listOf("  /resume ", "/usage", " /clear")
        val serialized = QuickSnippetsManager.serializeSnippets(snippets)
        assertEquals("/resume\n/usage\n/clear", serialized)
    }

    @Test
    fun `addSnippet appends new command without duplicates`() {
        val updated = QuickSnippetsManager.addSnippet(
            currentRaw = "/resume\n/usage",
            newSnippet = "/clear",
        )
        assertEquals("/resume\n/usage\n/clear", updated)
    }

    @Test
    fun `updateSnippet modifies target snippet and filters blank`() {
        val updated = QuickSnippetsManager.updateSnippet(
            currentRaw = "/resume\n/usage\n/clear",
            oldSnippet = "/usage",
            newSnippet = "/status",
        )
        assertEquals("/resume\n/status\n/clear", updated)
    }

    @Test
    fun `removeSnippet deletes target command`() {
        val updated = QuickSnippetsManager.removeSnippet(
            currentRaw = "/resume\n/usage\n/clear",
            targetSnippet = "/usage",
        )
        assertEquals("/resume\n/clear", updated)
    }
}
