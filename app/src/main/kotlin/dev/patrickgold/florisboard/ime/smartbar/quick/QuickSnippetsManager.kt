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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manager and utility functions for custom quick text snippets displayed in the Smartbar.
 * Provides parsing, insertion, and transactional editing of quick commands (e.g. /resume, /usage).
 */
object QuickSnippetsManager {

    /**
     * Parses raw newline-separated snippets string into a cleaned list of non-empty snippet strings.
     */
    fun parseSnippets(raw: String): List<String> {
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Serializes a list of snippets into the raw newline-separated string for persistence.
     */
    fun serializeSnippets(snippets: List<String>): String {
        return snippets
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /**
     * Appends a new snippet to the current stored snippets and returns the updated serialized string.
     */
    fun addSnippet(currentRaw: String, newSnippet: String): String {
        val trimmed = newSnippet.trim()
        if (trimmed.isEmpty()) return currentRaw
        val currentList = parseSnippets(currentRaw)
        val updated = (currentList + trimmed).distinct()
        return serializeSnippets(updated)
    }

    /**
     * Updates an existing snippet, or removes it if the new value is blank, returning the serialized string.
     */
    fun updateSnippet(currentRaw: String, oldSnippet: String, newSnippet: String): String {
        val trimmed = newSnippet.trim()
        val currentList = parseSnippets(currentRaw)
        val updatedList = currentList.map { if (it == oldSnippet) trimmed else it }.filter { it.isNotBlank() }
        return serializeSnippets(updatedList)
    }

    /**
     * Removes a snippet from the list and returns the updated serialized string.
     */
    fun removeSnippet(currentRaw: String, targetSnippet: String): String {
        val currentList = parseSnippets(currentRaw)
        val updatedList = currentList.filter { it != targetSnippet }
        return serializeSnippets(updatedList)
    }

    /**
     * Appends a new snippet and launches persistence in the given scope.
     */
    fun addSnippet(
        currentRaw: String,
        newSnippet: String,
        scope: CoroutineScope,
        saveAction: suspend (String) -> Unit,
    ) {
        val updated = addSnippet(currentRaw, newSnippet)
        scope.launch {
            saveAction(updated)
        }
    }

    /**
     * Updates an existing snippet and launches persistence in the given scope.
     */
    fun updateSnippet(
        currentRaw: String,
        oldSnippet: String,
        newSnippet: String,
        scope: CoroutineScope,
        saveAction: suspend (String) -> Unit,
    ) {
        val updated = updateSnippet(currentRaw, oldSnippet, newSnippet)
        scope.launch {
            saveAction(updated)
        }
    }

    /**
     * Removes a snippet and launches persistence in the given scope.
     */
    fun removeSnippet(
        currentRaw: String,
        targetSnippet: String,
        scope: CoroutineScope,
        saveAction: suspend (String) -> Unit,
    ) {
        val updated = removeSnippet(currentRaw, targetSnippet)
        scope.launch {
            saveAction(updated)
        }
    }
}
