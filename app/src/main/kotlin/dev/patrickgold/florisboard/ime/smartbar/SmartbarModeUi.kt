/*
 * Copyright (C) 2026 ARH Whisper Board Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggText

enum class SmartbarMode(val label: String, val badge: String) {
    CANDIDATES("Suggestions", "🔤"),
    SYMBOLS("Symbols & Email", "@/?"),
    SNIPPETS("Quick Snippets", "⚡");

    fun next(): SmartbarMode = when (this) {
        CANDIDATES -> SYMBOLS
        SYMBOLS -> SNIPPETS
        SNIPPETS -> CANDIDATES
    }
}

object SmartbarModeState {
    val mode = MutableStateFlow(SmartbarMode.CANDIDATES)

    fun cycle() {
        mode.value = mode.value.next()
    }
}

@Composable
fun SmartbarModeToggleChip(modifier: Modifier = Modifier) {
    val currentMode by SmartbarModeState.mode.collectAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { SmartbarModeState.cycle() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = currentMode.badge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SymbolsRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val editorInstance by context.editorInstance()
    val rawSymbols by prefs.smartbar.symbolShortcuts.collectAsState()
    val symbols = remember(rawSymbols) {
        rawSymbols.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (sym in symbols) {
            SnyggBox(
                elementName = FlorisImeUi.SmartbarCandidateWord.elementName,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        editorInstance.commitText(sym)
                    }
                    .padding(horizontal = 10.dp),
            ) {
                SnyggText(
                    elementName = FlorisImeUi.SmartbarCandidateWord.elementName,
                    text = sym,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
fun QuickSnippetsRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val editorInstance by context.editorInstance()
    val scope = rememberCoroutineScope()
    val rawSnippets by prefs.smartbar.quickSnippets.collectAsState()
    val snippets = remember(rawSnippets) {
        dev.patrickgold.florisboard.ime.smartbar.quick.QuickSnippetsManager.parseSnippets(rawSnippets)
    }
    val scrollState = rememberScrollState()

    var editingSnippet by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSnippetText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (snippet in snippets) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = { editorInstance.commitText(snippet) },
                        onLongClick = { editingSnippet = snippet },
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Add snippet button [+]
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    newSnippetText = ""
                    showAddDialog = true
                }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+ Add",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    if (showAddDialog) {
        JetPrefAlertDialog(
            title = "Add Quick Snippet",
            confirmLabel = stringRes(R.string.action__add),
            dismissLabel = stringRes(R.string.action__cancel),
            onDismiss = { showAddDialog = false },
            onConfirm = {
                dev.patrickgold.florisboard.ime.smartbar.quick.QuickSnippetsManager.addSnippet(
                    rawSnippets,
                    newSnippetText,
                    scope,
                ) { prefs.smartbar.quickSnippets.set(it) }
                showAddDialog = false
            },
        ) {
            OutlinedTextField(
                value = newSnippetText,
                onValueChange = { newSnippetText = it },
                label = { Text("Snippet (e.g. /resume)") },
                singleLine = true,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    editingSnippet?.let { target ->
        var editText by remember { mutableStateOf(target) }
        JetPrefAlertDialog(
            title = "Edit or Delete Snippet",
            confirmLabel = stringRes(R.string.action__save),
            dismissLabel = stringRes(R.string.action__delete),
            onDismiss = { editingSnippet = null },
            onConfirm = {
                dev.patrickgold.florisboard.ime.smartbar.quick.QuickSnippetsManager.updateSnippet(
                    rawSnippets,
                    target,
                    editText,
                    scope,
                ) { prefs.smartbar.quickSnippets.set(it) }
                editingSnippet = null
            },
        ) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                label = { Text("Snippet") },
                singleLine = true,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
