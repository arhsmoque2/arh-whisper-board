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

package dev.patrickgold.florisboard.ime.clipboard

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import java.io.File

object MarkdownExportUtil {

    /**
     * Extracts an H1 heading title from the markdown content.
     * Falls back to HTML <h1>, then the first non-empty line, then a timestamp.
     */
    fun extractH1Title(markdown: String): String {
        val mdMatch = Regex("""^#\s+([^\r\n]+)""", RegexOption.MULTILINE).find(markdown)
        if (mdMatch != null) {
            val raw = mdMatch.groupValues[1].trim()
            if (raw.isNotEmpty()) return sanitizeFileName(raw)
        }

        val htmlMatch = Regex("""<h1[^>]*>(.*?)</h1>""", RegexOption.IGNORE_CASE).find(markdown)
        if (htmlMatch != null) {
            val raw = htmlMatch.groupValues[1].trim()
            if (raw.isNotEmpty()) return sanitizeFileName(raw)
        }

        val firstLine = markdown.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        return sanitizeFileName(firstLine?.take(40) ?: "clip_${System.currentTimeMillis()}")
    }

    /**
     * Sanitizes strings into valid filesystem file names.
     */
    fun sanitizeFileName(raw: String): String =
        raw.trim().replace(Regex("""[\\/:*?"<>|#\r\n\t]"""), "_").take(50)

    /**
     * Exports a clipboard item to a .md file in the public Downloads/ARH-Notes folder.
     */
    fun exportToMarkdown(context: Context, item: ClipboardItem): Uri? {
        val text = item.text ?: return null
        val title = extractH1Title(text)
        val fileName = "$title.md"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ARH-Notes")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Exported to Downloads/ARH-Notes/$fileName", Toast.LENGTH_SHORT).show()
                }
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ARH-Notes")
                dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(text, Charsets.UTF_8)
                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
